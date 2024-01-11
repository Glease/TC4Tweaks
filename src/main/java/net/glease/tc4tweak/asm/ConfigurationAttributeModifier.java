package net.glease.tc4tweak.asm;

import java.util.UUID;

import net.glease.tc4tweak.ConfigurationHandler;
import net.minecraft.entity.ai.attributes.AttributeModifier;

public class ConfigurationAttributeModifier extends AttributeModifier {

    @Callhook(adder = EntityUtilsVisitor.class, module = ASMConstants.Modules.ChampionModConfig)
    public ConfigurationAttributeModifier(UUID p_i1606_1_, String p_i1606_2_, double p_i1606_3_, int p_i1606_5_) {
        super(p_i1606_1_, p_i1606_2_, p_i1606_3_, p_i1606_5_);
    }

    @Override
    public double getAmount() {
        return ConfigurationHandler.INSTANCE.getChampionModValue(getID(), super.getAmount());
    }
}
