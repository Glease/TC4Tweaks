package net.glease.tc4tweak.asm;

import net.glease.tc4tweak.ClientProxy;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.tiles.TileMagicWorkbench;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ASMCallhook {
	private static final List<WeakReference<TileMagicWorkbench>> postponed = new ArrayList<>();
	// workbench throttling
	private static long lastUpdate = 0;

	public static void updatePostponed() {
		synchronized (postponed) {
			for (WeakReference<TileMagicWorkbench> workbench : postponed) {
				TileMagicWorkbench tile = workbench.get();
				if (tile != null && tile.eventHandler != null && !tile.isInvalid() && tile.hasWorldObj()) {
					// best effort guess on whether tile is valid
					tile.eventHandler.onCraftMatrixChanged(tile);
				}
			}
			postponed.clear();
		}
	}

	/**
	 * called from GuiResearchTable. first arg is this
	 */
	@Callhook
	public static void handleMouseInput(GuiResearchTable screen) {
		ClientProxy.handleMouseInput(screen);
	}

	/**
	 * Throttle the amount of arcane workbench update on client side
	 * called from TileMagicWorkbench.
	 */
	@Callhook
	public static void updateCraftingMatrix(TileMagicWorkbench self) {
		if (!self.getWorldObj().isRemote) {
			self.eventHandler.onCraftMatrixChanged(self);
			return;
		}
		long oldUpdate = lastUpdate;
		lastUpdate = System.currentTimeMillis();
		if (lastUpdate - oldUpdate > 1000 / 5) {
			self.eventHandler.onCraftMatrixChanged(self); // 5 times per second at max
		} else {
			synchronized (postponed) {
				postponed.add(new WeakReference<>(self));
			}
		}
	}

}
