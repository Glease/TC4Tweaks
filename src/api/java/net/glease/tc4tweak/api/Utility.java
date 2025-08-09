package net.glease.tc4tweak.api;

import java.util.Iterator;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

class Utility {
    static void mergeNBTTags(NBTTagCompound source, NBTTagCompound dest, Set<String> whitelist) {
        for (@SuppressWarnings("unchecked") Iterator<String> iterator = source.func_150296_c().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (whitelist != null && !whitelist.contains(key)) continue;
            NBTBase sourceTag = source.getTag(key);
            if (!dest.hasKey(key)) {
                dest.setTag(key, sourceTag.copy());
            } else {
                NBTBase destTag = dest.getTag(key);
                if (sourceTag.getId() != destTag.getId()) {
                    // not very good.. we let dest take precedence
                    continue;
                }
                switch (sourceTag.getId()) {
                    case Constants.NBT.TAG_LIST:
                        NBTTagList destTagList = (NBTTagList) destTag;
                        NBTTagList sourceTagList = (NBTTagList) sourceTag.copy();
                        if (destTagList.func_150303_d() != sourceTagList.func_150303_d()) {
                            continue;
                        }
                        // this is probably not very efficient, but should be good enough :tm:
                        while (sourceTagList.tagCount() > 0) {
                            destTagList.appendTag(sourceTagList.removeTag(0));
                        }
                        break;
                    case Constants.NBT.TAG_COMPOUND:
                        mergeNBTTags((NBTTagCompound) sourceTag, (NBTTagCompound) destTag, null);
                        break;
                    default:
                        // conflict - let dest take precedence
                }
            }
        }
    }
}
