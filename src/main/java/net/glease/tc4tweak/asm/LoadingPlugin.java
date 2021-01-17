package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions("net.glease.tc4tweak.asm")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("TC4TweakCoreMod")
public class LoadingPlugin implements IFMLLoadingPlugin {
	static boolean dev;
	static boolean gt6;

	@Override
	public String[] getASMTransformerClass() {
		return new String[]{"net.glease.tc4tweak.asm.TC4Transformer"};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		dev = !(boolean) data.get("runtimeDeobfuscationEnabled");
		gt6 = ((List<?>) data.get("coremodList")).stream().anyMatch(o -> o.toString().contains("Greg-ASM"));
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
