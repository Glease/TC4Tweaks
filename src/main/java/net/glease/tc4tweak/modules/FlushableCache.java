package net.glease.tc4tweak.modules;

import gnu.trove.map.TIntObjectMap;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FlushableCache<E> {
    private static final List<WeakReference<FlushableCache<?>>> allCaches = new CopyOnWriteArrayList<>();
    private static volatile boolean enabled = false;
    private E cache;

    protected FlushableCache() {
        allCaches.add(new WeakReference<>(this));
        if (enabled)
            populate(true);
    }

    private static void cleanStale() {
        allCaches.removeIf(r -> r.get() == null);
    }

    public static void enableAll(boolean doCreate) {
        allCaches.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(flushableCache -> flushableCache.populate(doCreate));
        enabled = true;
        cleanStale();
    }

    public static void disableAll() {
        allCaches.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(FlushableCache::clear);
        enabled = false;
    }

    protected static <T, C extends TIntObjectMap<T>> C mergeTIntObjectMap(C lhs, C rhs) {
        lhs.putAll(rhs);
        return lhs;
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
}
