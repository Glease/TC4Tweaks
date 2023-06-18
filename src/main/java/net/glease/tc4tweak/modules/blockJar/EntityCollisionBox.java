package net.glease.tc4tweak.modules.blockJar;

import net.glease.tc4tweak.network.NetworkedConfiguration;

public class EntityCollisionBox {
    private static final float[] VANILLA_PARAMETERS = {
        0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F
    };
    private static final float[] SMALLER_PARAMETERS = {
        0.1875F, 0.0F, 0.1875F, 0.8125F, 0.75F, 0.8125F
    };

    public static float getBlockJarEntityCollisionBoxParameter(int index) {
        if (NetworkedConfiguration.isSmallerJar())
            return SMALLER_PARAMETERS[index];
        else
            return VANILLA_PARAMETERS[index];
    }
}
