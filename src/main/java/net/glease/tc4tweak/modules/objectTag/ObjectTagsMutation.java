package net.glease.tc4tweak.modules.objectTag;

import gnu.trove.map.TIntObjectMap;
import thaumcraft.api.aspects.AspectList;

interface ObjectTagsMutation {
	void accept(TIntObjectMap<AspectList> submap, int meta);
}
