package ru.test.impl.cache;

import java.util.Date;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import static ru.test.impl.cache.Cache.RemoveReason.MANUAL;
import static ru.test.impl.cache.CacheManagerImpl.DEFAULT_EXPIRY_TIME;
import static ru.test.impl.cache.CacheManagerImpl.now;

public class CacheImpl<K, V> implements Cache<K, V> {

    private ConcurrentMap<K, CacheItem<V>> cache = new ConcurrentHashMap<>();

    private BiConsumer<K, V> cacheWriter;

    private RemovalListener<K, V> removalListener;

    private TreeMap<Long, Set<K>> timeScale = new TreeMap<>();

    CacheImpl() {
    }

    @Override
    public V get(K key) {
        CacheItem<V> cacheItem = cache.get(key);
        if (cacheItem == null || cacheItem.time < now()) {
            return null;
        }
        return cacheItem.value;
    }

    V getIgnoreExpired(K key) {
        CacheItem<V> cacheItem = cache.get(key);
        return cacheItem == null ? null : cacheItem.value;
    }

    @Override
    public void put(K key, V value) {
        put(key, value, DEFAULT_EXPIRY_TIME);
    }

    @Override
    public void put(K key, V value, Date expiryDate) {
        put(key, value, expiryDate.getTime() / 1000L);
    }

    private void put(K key, V value, long expiryTime) {
        CacheItem<V> cacheItem = new CacheItem<>(value, expiryTime);
        cache.put(key, cacheItem);
        CacheManagerImpl.persistCacheValue(this, key, value, cacheItem.time);
    }

    @Override
    public void remove(K key) {
        CacheItem<V> cacheItem = cache.get(key);
        cache.remove(key);
        Set<K> keysSet = timeScale.get(cacheItem.time);
        keysSet.remove(key);
        if (removalListener != null) {
            removalListener.accept(key, cacheItem.value, MANUAL);
        }
    }

    void removeValueOnly(K key) {
        cache.remove(key);
    }

    @Override
    public void setCacheWriter(BiConsumer<K, V> cacheWriter) {
        this.cacheWriter = cacheWriter;
    }

    BiConsumer<K, V> getCacheWriter() {
        return cacheWriter;
    }

    @Override
    public void setRemovalListener(RemovalListener<K, V> removalListener) {
        this.removalListener = removalListener;
    }

    RemovalListener<K, V> getRemovalListener() {
        return removalListener;
    }

    TreeMap<Long, Set<K>> getTimeScale() {
        return timeScale;
    }

    static class CacheItem<T> {

        private T value;

        private long time;

        public CacheItem(T value, long time) {
            this.value = value;
            this.time = time;
        }
    }
}
