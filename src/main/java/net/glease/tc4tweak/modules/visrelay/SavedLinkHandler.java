package net.glease.tc4tweak.modules.visrelay;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.glease.tc4tweak.CommonUtils;
import net.glease.tc4tweak.ConfigurationHandler;
import net.glease.tc4tweak.asm.ITileVisNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;

import static net.glease.tc4tweak.TC4Tweak.log;

public class SavedLinkHandler {
    private enum Action {
        RETURN(true),
        SET_PARENT_RETURN(true),
        CLEAR_CONTINUE(false),
        DISABLED(false),
        ;
        private final boolean decision;
        Action(boolean decision) {
            this.decision = decision;
        }

        public boolean returnValue() {
            return decision;
        }
    }

    private static String getNodeType(TileVisNode node) {
        return node.isSource() ? "Source" : "Relay";
    }

    // true -> RET, false -> resume
    public static boolean processSavedLink(ITileVisNode visNode) {
        TileVisNode node = (TileVisNode) visNode;
        WorldCoordinates c = new WorldCoordinates(node);
        Action action;
        try {
            action = processSavedLink0(visNode);
        } catch (Exception e) {
            log.error("Failed to process saved link. Defaulting to no saved link!", e);
            visNode.clearSavedLink();
            return false;
        }
        if (action != Action.DISABLED) {
            log.info("Processed saved link for node {} at {},{},{}: {}", getNodeType(node), c.x, c.y, c.z, action);
        }
        return action.returnValue();
    }

    private static Action processSavedLink0(ITileVisNode visNode) {
        List<ChunkCoordinates> link = visNode.getSavedLink();
        if (link == null) return Action.DISABLED;
        TileVisNode node = (TileVisNode) visNode;
        ChunkCoordinates c = link.get(0);
        World w = node.getWorldObj();
        if (!w.blockExists(c.posX, c.posY, c.posZ)) {
            return Action.RETURN;
        }
        TileEntity tile = w.getTileEntity(c.posX, c.posY, c.posZ);
        if (!canConnect(node, tile)) {
            // ThE uses a fake TE for cv p2p that is not retrievable via getTileEntity
            // however it's accessible via VisNetHandler.sources
            HashMap<WorldCoordinates, WeakReference<TileVisNode>> sourcelist = VisNetHandler.sources.get(w.provider.dimensionId);
            TileVisNode sourcenode = CommonUtils.deref(sourcelist.get(new WorldCoordinates(c.posX, c.posY, c.posZ, w.provider.dimensionId)));
            if (sourcenode == null) {
                visNode.clearSavedLink();
                return Action.CLEAR_CONTINUE;
            }
            tile = sourcenode;
        }
        TileVisNode next = (TileVisNode) tile;
        if (next.isSource()) {
            SetParentHelper.setParent(next, node);
            w.markBlockForUpdate(node.xCoord, node.yCoord, node.zCoord);
            node.parentChanged();
            visNode.clearSavedLink();
            return Action.SET_PARENT_RETURN;
        }
        ITileVisNode nextExt = (ITileVisNode) next;
        List<ChunkCoordinates> nextLink = nextExt.getSavedLink();
        if (nextLink == null) {
            visNode.clearSavedLink();
            if (VisNetHandler.isNodeValid(next.getRootSource())) {
                SetParentHelper.setParent(next, node);
                w.markBlockForUpdate(node.xCoord, node.yCoord, node.zCoord);
                node.parentChanged();
                return Action.SET_PARENT_RETURN;
            } else {
                return Action.CLEAR_CONTINUE;
            }
        }
        if (link.size() == 1 || nextLink.get(0).equals(link.get(1))) {
            return Action.RETURN;
        }
        visNode.clearSavedLink();
        return Action.CLEAR_CONTINUE;
    }

    private static boolean canConnect(TileVisNode node, TileEntity tile) {
        if (!(tile instanceof TileVisNode)) return false;
        TileVisNode next = (TileVisNode) tile;
        if (VisNetHandler.canNodeBeSeen(node, next)) return true;
        return node.getAttunement() == -1 || next.getAttunement() == -1 || next.getAttunement() == node.getAttunement();
    }

    public static List<ChunkCoordinates> readFromNBT(TileVisNode thiz, NBTTagCompound tag) {
        if (thiz.isSource() || !tag.hasKey("Link") || !ConfigurationHandler.INSTANCE.isSavedLinkEnabled()) {
            return null;
        }
        NBTTagList linkRaw = tag.getTagList("Link", Constants.NBT.TAG_COMPOUND);
        log.trace("Reading link for node {} at {},{},{}. {} nodes.", getNodeType(thiz), thiz.xCoord, thiz.yCoord, thiz.zCoord, linkRaw.tagCount());
        List<ChunkCoordinates> link = new ArrayList<>();
        int end = Math.min(linkRaw.tagCount(), 2);
        for (int i = 0; i < end; i++) {
            link.add(readOne(linkRaw.getCompoundTagAt(i)));
        }
        return link;
    }

    public static void writeToNBT(TileVisNode thiz, NBTTagCompound tag) {
        if (thiz.isSource() || !ConfigurationHandler.INSTANCE.isSavedLinkEnabled()) return;
        TileVisNode root = CommonUtils.deref(thiz.getRootSource());
        if (root == null)
            return;
        NBTTagList path = new NBTTagList();
        TileVisNode node = CommonUtils.deref(thiz.getParent());
        // historically we store the whole path up to source node (hence the name link
        // but it turns out we only use 2 nodes. more ancient ancestors are prone to all kinds of weirdness
        // due to unloading order, but 2 nodes seem to stable enough
        while (node != null && (path.tagCount() <= 1 || ConfigurationHandler.INSTANCE.isSavedLinkSaveWholeLink())) {
            path.appendTag(writeOne(node));
            node = CommonUtils.deref(node.getParent());
        }
        tag.setTag("Link", path);
        log.trace("Written link for node {} at {},{},{}. {} element.", getNodeType(thiz), thiz.xCoord, thiz.yCoord, thiz.zCoord, path.tagCount());
    }

    private static NBTTagCompound writeOne(TileVisNode node) {
        NBTTagCompound elem = new NBTTagCompound();
        elem.setInteger("x", node.xCoord);
        elem.setInteger("y", node.yCoord);
        elem.setInteger("z", node.zCoord);
        return elem;
    }

    private static ChunkCoordinates readOne(NBTTagCompound elem) {
        return new ChunkCoordinates(elem.getInteger("x"), elem.getInteger("y"), elem.getInteger("z"));
    }
}
