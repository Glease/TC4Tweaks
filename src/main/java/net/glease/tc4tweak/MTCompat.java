package net.glease.tc4tweak;

import minetweaker.MineTweakerImplementationAPI;
import net.glease.tc4tweak.asm.ASMCallhookServer;

public class MTCompat {
	public static void preInit() {
		MineTweakerImplementationAPI.onReloadEvent(e -> ASMCallhookServer.preventAllCache());
		MineTweakerImplementationAPI.onPostReload(e -> ASMCallhookServer.flushAllCache());
	}
}
