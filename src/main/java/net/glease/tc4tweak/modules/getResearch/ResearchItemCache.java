package net.glease.tc4tweak.modules.getResearch;

import net.glease.tc4tweak.modules.FlushableCache;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResearchItemCache extends FlushableCache<Map<String, ResearchItem>> {
	@Override
	protected Map<String, ResearchItem> createCache() {
		return ResearchCategories.researchCategories.values().stream()
				.flatMap(l -> l.research.values().stream())
				.collect(Collectors.toMap(
						i -> i.key,
						Function.identity(),
						(u, v) -> u,
						LinkedHashMap::new
				));
	}
}
