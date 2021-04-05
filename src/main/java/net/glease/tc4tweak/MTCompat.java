package net.glease.tc4tweak;

import minetweaker.MineTweakerImplementationAPI;
import net.glease.tc4tweak.modules.FlushableCache;

public class MTCompat {
	public static void preInit() {
		MineTweakerImplementationAPI.onReloadEvent(e -> FlushableCache.disableAll());
		MineTweakerImplementationAPI.onPostReload(e -> FlushableCache.enableAll(true));
	}
}
