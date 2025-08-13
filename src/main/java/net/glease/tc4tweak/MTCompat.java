package net.glease.tc4tweak;

import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.MineTweakerImplementationAPI.ReloadEvent;
import net.glease.tc4tweak.modules.FlushableCache;
import net.glease.tc4tweak.modules.infusionRecipe.MTCompatForInfusionExt;

public class MTCompat {
    public static void preInit() {
        MineTweakerImplementationAPI.onReloadEvent(e -> FlushableCache.disableAll());
        MineTweakerImplementationAPI.onPostReload(MTCompat::onPostReload);
        MineTweakerAPI.registerClass(MTCompatForInfusionExt.class);
    }

    private static void onPostReload(ReloadEvent e) {
        FlushableCache.enableAll(true);
        MineTweakerAPI.apply(new IUndoableAction() {
            @Override
            public void apply() {

            }

            @Override
            public boolean canUndo() {
                return true;
            }

            @Override
            public void undo() {
                FlushableCache.disableAll();
            }

            @Override
            public String describe() {
                return "Dummy action for catching reload event";
            }

            @Override
            public String describeUndo() {
                return "Dummy action for catching reload event";
            }

            @Override
            public Object getOverrideKey() {
                return null;
            }
        });
    }
}
