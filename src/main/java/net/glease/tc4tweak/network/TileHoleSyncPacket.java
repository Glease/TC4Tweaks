package net.glease.tc4tweak.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileHole;

import java.io.IOException;

public class TileHoleSyncPacket implements IMessage, IMessageHandler<TileHoleSyncPacket, IMessage> {
    private S35PacketUpdateTileEntity origin;

    public TileHoleSyncPacket() {
    }

    public TileHoleSyncPacket(S35PacketUpdateTileEntity origin) {
        this.origin = origin;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        origin = new S35PacketUpdateTileEntity();
        try {
            origin.readPacketData(new PacketBuffer(buf));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            origin.writePacketData(new PacketBuffer(buf));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IMessage onMessage(TileHoleSyncPacket message, MessageContext ctx) {
        WorldClient theWorld = Minecraft.getMinecraft().theWorld;
        if (theWorld == null) return null;
        int x = message.origin.func_148856_c();
        int y = message.origin.func_148855_d();
        int z = message.origin.func_148854_e();
        if (!theWorld.blockExists(x, y, z)) return null;
        if (theWorld.getBlock(x, y, z) != ConfigBlocks.blockHole) return null;
        TileHole t = new TileHole();
        theWorld.setTileEntity(x, y, z, t);
        ctx.getClientHandler().handleUpdateTileEntity(message.origin);
        return null;
    }
}
