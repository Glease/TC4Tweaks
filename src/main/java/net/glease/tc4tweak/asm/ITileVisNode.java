package net.glease.tc4tweak.asm;

import java.util.List;

import net.minecraft.util.ChunkCoordinates;

public interface ITileVisNode {
    List<ChunkCoordinates> getSavedLink();
    void clearSavedLink();
}
