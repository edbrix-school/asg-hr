package com.asg.hr.common.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Small time based cache. User rights and the employee a login is linked to change rarely, while
 * the list screens that read them are called constantly, so both are held for a short while rather
 * than fetched per request.
 */
class TtlCache<K, V> {

    private record Entry<V>(V value, long expiresAt) {
        boolean isFresh() {
            return System.currentTimeMillis() < expiresAt;
        }
    }

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final int maxEntries;

    TtlCache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /** Cached value for {@code key}, loading it when absent or stale. A ttl of 0 disables caching. */
    V get(K key, long ttlSeconds, Supplier<V> loader) {
        if (ttlSeconds <= 0) {
            return loader.get();
        }

        Entry<V> entry = entries.get(key);
        if (entry != null && entry.isFresh()) {
            return entry.value();
        }

        V value = loader.get();
        if (entries.size() >= maxEntries) {
            entries.values().removeIf(existing -> !existing.isFresh());
            if (entries.size() >= maxEntries) {
                // everything is still fresh: start over rather than grow past the limit
                entries.clear();
            }
        }
        entries.put(key, new Entry<>(value, System.currentTimeMillis() + ttlSeconds * 1000L));
        return value;
    }

    void evict(K key) {
        entries.remove(key);
    }

    void clear() {
        entries.clear();
    }

    int size() {
        return entries.size();
    }
}
