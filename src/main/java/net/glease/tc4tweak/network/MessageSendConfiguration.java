package net.glease.tc4tweak.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.glease.tc4tweak.ConfigurationHandler;

public class MessageSendConfiguration implements IMessage, IMessageHandler<MessageSendConfiguration, IMessage> {
    private boolean checkWorkbenchRecipes;

    public MessageSendConfiguration() {
        this.checkWorkbenchRecipes = ConfigurationHandler.INSTANCE.isCheckWorkbenchRecipes();
    }

    public MessageSendConfiguration(boolean checkWorkbenchRecipes) {
        this.checkWorkbenchRecipes = checkWorkbenchRecipes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        checkWorkbenchRecipes = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(checkWorkbenchRecipes);
    }

    @Override
    public IMessage onMessage(MessageSendConfiguration message, MessageContext ctx) {
        NetworkedConfiguration.checkWorkbenchRecipes = checkWorkbenchRecipes;
        return null;
    }
}
