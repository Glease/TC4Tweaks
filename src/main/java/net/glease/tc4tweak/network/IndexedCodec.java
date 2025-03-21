// SPDX-License-Identifier: LGPL-2.1
/**
 * Copyright 2014, FML authors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.glease.tc4tweak.network;

import java.lang.ref.WeakReference;
import java.util.List;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.TObjectByteMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import static cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER;
import static net.glease.tc4tweak.TC4Tweak.log;

@ChannelHandler.Sharable
public class IndexedCodec extends MessageToMessageCodec<FMLProxyPacket, IMessage> {
    private final TByteObjectMap<Class<? extends IMessage>> discriminators = new TByteObjectHashMap<>();
    private final TObjectByteMap<Class<? extends IMessage>> types = new TObjectByteHashMap<>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        ctx.attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<>());
    }

    public IndexedCodec addDiscriminator(int discriminator, Class<? extends IMessage> type) {
        discriminators.put((byte) discriminator, type);
        types.put(type, (byte) discriminator);
        return this;
    }

    @Override
    protected final void encode(ChannelHandlerContext ctx, IMessage msg, List<Object> out) throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        Class<? extends IMessage> clazz = msg.getClass();
        int discriminator = types.get(clazz);
        buffer.writeByte(discriminator);
        msg.toBytes(buffer);
        FMLProxyPacket proxy = new FMLProxyPacket(buffer.copy(), ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get());
        WeakReference<FMLProxyPacket> ref = ctx.attr(INBOUNDPACKETTRACKER).get().get();
        FMLProxyPacket old = ref == null ? null : ref.get();
        if (old != null) {
            proxy.setDispatcher(old.getDispatcher());
        }
        out.add(proxy);
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
        ByteBuf payload = msg.payload();
        byte discriminator = payload.readByte();
        Class<? extends IMessage> clazz = discriminators.get(discriminator);
        if (clazz == null) {
            // TC4Tweaks modification: don't just panic about this. this is fine
            log.warn("Undefined message: {}. Is server running a newer TC4Tweaks?", discriminator);
            return;
        }
        IMessage newMsg = clazz.newInstance();
        ctx.attr(INBOUNDPACKETTRACKER).get().set(new WeakReference<>(msg));
        newMsg.fromBytes(payload);
        out.add(newMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("IndexedCodec exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
