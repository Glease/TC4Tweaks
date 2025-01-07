package net.glease.tc4tweak.modules.visrelay;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.reflect.FieldUtils;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;

import static net.glease.tc4tweak.TC4Tweak.log;

public class SetParentHelper {
    private static final ArrayList<WorldCoordinates> cache;
    private static final HashMap<WorldCoordinates, ArrayList<WeakReference<TileVisNode>>> nearbyNodes;

    static {
        try {
            cache = (ArrayList<WorldCoordinates>) FieldUtils.getField(VisNetHandler.class, "cache", true).get(null);
            nearbyNodes = (HashMap<WorldCoordinates, ArrayList<WeakReference<TileVisNode>>>) FieldUtils.getField(VisNetHandler.class, "nearbyNodes", true).get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static void setParent(TileVisNode parent, TileVisNode child) {
        log.trace("Force set parent of {} ({},{},{}) to {} ({},{},{})", child.getClass().getSimpleName(), child.xCoord, child.yCoord, child.zCoord, parent.getClass().getSimpleName(), parent.xCoord, parent.yCoord, parent.zCoord);
        WeakReference<TileVisNode> ref = new WeakReference<>(child);
        child.setParent(new WeakReference<>(parent));
        parent.getChildren().add(ref);
        nearbyNodes.clear();
        cache.clear();
    }
}
