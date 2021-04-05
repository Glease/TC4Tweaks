package net.glease.tc4tweak.modules;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FlushableCache<E> {
	private static final List<WeakReference<FlushableCache<?>>> allCaches = new CopyOnWriteArrayList<>();
	protected E cache;

	protected FlushableCache() {
		register(this);
	}

	private static void cleanStale() {
		allCaches.removeIf(r -> r.get() == null);
	}

	protected void populate(boolean doCreate) {
		if (doCreate || cache == null)
			cache = createCache();
	}

	protected abstract E createCache();

	protected void clear() {
		cache = null;
	}

	public boolean isEnabled() {
		return cache != null;
	}

	public E getCache() {
		return cache;
	}

	public static void register(FlushableCache<?> cache) {
		allCaches.add(new WeakReference<>(cache));
	}

	public static void enableAll(boolean doCreate) {
		allCaches.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(flushableCache -> flushableCache.populate(doCreate));
		cleanStale();
	}

	public static void disableAll() {
		allCaches.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(FlushableCache::clear);
	}
}
