package net.glease.tc4tweak.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.modules.infusionRecipe.InfusionOreDictMode;
import net.minecraft.nbt.NBTTagCompound;

public class MessageSendConfigurationV2 implements IMessage,  IMessageHandler<MessageSendConfigurationV2, IMessage>  {
    private NBTTagCompound tag;

    public MessageSendConfigurationV2() {
        tag = new NBTTagCompound();
        // yeah I said NBT is an unfortunate piece of tech, but it does give us a bit of flexibility over network
        // protocol
        tag.setBoolean("sj", ConfigurationHandler.INSTANCE.isSmallerJars());
        tag.setString("om", ConfigurationHandler.INSTANCE.getInfusionOreDictMode().name());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tag = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, tag);
    }

    @Override
    public IMessage onMessage(MessageSendConfigurationV2 message, MessageContext ctx) {
        NetworkedConfiguration.smallerJar = message.tag.getBoolean("sj");
        NetworkedConfiguration.infusionOreDictMode = InfusionOreDictMode.get(message.tag.getString("om"));
        return null;
    }
}
