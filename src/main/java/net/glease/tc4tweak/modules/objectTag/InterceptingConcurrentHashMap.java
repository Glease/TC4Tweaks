package net.glease.tc4tweak.modules.objectTag;

import gnu.trove.map.TIntObjectMap;
import thaumcraft.api.aspects.AspectList;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@SuppressWarnings("rawtypes")
class InterceptingConcurrentHashMap extends ConcurrentHashMap<List, AspectList> {
	@Override
	public AspectList put(List key, AspectList value) {
		GetObjectTags.mutateObjectTagsSubmap(key, (submap, meta) -> submap.put(meta, value));
		return super.put(key, value);
	}

	@Override
	public AspectList remove(Object key) {
		if (key instanceof List) GetObjectTags.mutateObjectTagsSubmap((List<?>) key, TIntObjectMap::remove);
		return super.remove(key);
	}

	@Override
	public boolean remove(Object key, Object value) {
		if (key instanceof List && value instanceof AspectList) {
			GetObjectTags.mutateObjectTagsSubmap((List<?>) key, (submap, meta) -> {
				if (value.equals(submap.get(meta))) submap.remove(meta);
			});
		}
		return super.remove(key, value);
	}
}
