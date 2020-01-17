package ru.test.impl.cache;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static ru.test.impl.cache.Cache.RemoveReason.EXPIRED;

public class CacheManagerImpl implements CacheManager {

    static final long DEFAULT_EXPIRY_TIME = 29350666800L; // 2900-01-01 00:00:00
    static final long DEFAULT_EXPIRY_TIME_MILLIS = DEFAULT_EXPIRY_TIME * 1000L;

    private static final AtomicReference<Map<String, CacheImpl<?, ?>>> cacheMapReference = new AtomicReference<>();

    private static final ConcurrentLinkedQueue<PersistQueueItem<?, ?>> persistQueue = new ConcurrentLinkedQueue<>();

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        Map<String, CacheImpl<?, ?>> cacheMap = cacheMapReference.get();
        if (cacheMap == null) {
            return null;
        }
        return (Cache<K, V>) cacheMap.get(name);
    }

    static <K, V> void setCache(String name, CacheImpl<K, V> cache) {
        Map<String, CacheImpl<?, ?>> newCacheMap;
        Map<String, CacheImpl<?, ?>> cacheMap;
        do {
            cacheMap = cacheMapReference.get();
            newCacheMap = new HashMap<>();
            if (cacheMap != null) {
                newCacheMap.putAll(cacheMap);
            }
            newCacheMap.put(name, cache);
        } while (!cacheMapReference.compareAndSet(cacheMap, newCacheMap));
    }

    protected void startMaintenance() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                persistQueuedItems();
            }
        }).start();
    }

    private void persistQueuedItems() {
        PersistQueueItem<?, ?> persistQueueItem;
        while ((persistQueueItem = persistQueue.poll()) != null) {
            persistQueuedItem(persistQueueItem);
        }
    }

    private <K, V> void persistQueuedItem(PersistQueueItem<K, V> persistQueueItem) {
        CacheImpl<K, V> cache = persistQueueItem.cache;
        cache.getTimeScale().compute(persistQueueItem.time, (key, set) -> {
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(persistQueueItem.key);
            return set;
        });
        BiConsumer<K, V> cacheWriter = cache.getCacheWriter();
        if (cacheWriter != null) {
            cacheWriter.accept(persistQueueItem.key, persistQueueItem.value);
        }
    }

    protected void removeExpiredItems() {
        long now = now();
        Map<String, CacheImpl<?, ?>> cacheMap = cacheMapReference.get();
        if (cacheMap != null) {
            for (CacheImpl<?, ?> cache : cacheMap.values()) {
                removeExpiredCacheItems(cache, now);
            }
        }
    }

    private <K, V> void removeExpiredCacheItems(CacheImpl<K, V> cache, long now) {
        TreeMap<Long, Set<K>> timeScale = cache.getTimeScale();
        while (!timeScale.isEmpty() && timeScale.firstKey() < now) {
            Map.Entry<Long, Set<K>> entry = timeScale.pollFirstEntry();
            Cache.RemovalListener<K, V> removalListener = cache.getRemovalListener();
            for (K key : entry.getValue()) {
                V expiredValue = cache.getIgnoreExpired(key);
                cache.removeValueOnly(key);
                if (removalListener != null) {
                    removalListener.accept(key, expiredValue, EXPIRED);
                }
            }
        }
    }

    static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    static <K, V> void persistCacheValue(CacheImpl<K, V> cache, K key, V value, long time) {
        PersistQueueItem<K, V> item = new PersistQueueItem<>(cache, key, value, time);
        persistQueue.add(item);
    }

    private static class PersistQueueItem<K, V> {

        private CacheImpl<K, V> cache;

        private K key;

        private V value;

        private long time;

        public PersistQueueItem(CacheImpl<K, V> cache, K key, V value, long time) {
            this.cache = cache;
            this.key = key;
            this.value = value;
            this.time = time;
        }
    }
}
