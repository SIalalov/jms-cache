package ru.test.impl.cache;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;
import static org.junit.Assert.fail;

public class CacheManagerImplTest {

    /**
     * Проверка потокобезопасности
     */
    @Test
    public void testSuccess() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            CacheBuilder.<String, String>create(String.valueOf(i))
                    .withCacheWriter((k, v) -> {
                        try {
                            sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    })
                    .withRemovalListener((key, value, reason) -> {
                        try {
                            sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    }).build();
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(8);
        List<Future<Void>> futures = new ArrayList<>();

        final CacheManager cacheManager = new CacheManagerImpl();

        for (int i = 1; i <= 100; i++) {
            for (int j = 1; j <= 100; j++) {
                final String key = String.valueOf(i);
                final String value = String.valueOf(i);
                final String cacheName = String.valueOf(i % 10);
                futures.add(CompletableFuture.runAsync(() -> {
                    cacheManager.getCache(cacheName).put(key, value);
                    String cached = cacheManager.<String, String>getCache(cacheName).get(key);
                    //System.out.println("cache: " + cacheName + ", put: " + value + ", get: " + cached);
                    if (cached == null) {
                        throw new RuntimeException();
                    }
                }, threadPool));
            }
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        threadPool.shutdown();
    }

    /**
     * Проверка производительности
     */
    @Test
    public void testPerformance() {
        for (int i = 0; i < 10; i++) {
            CacheBuilder.<String, String>create(String.valueOf(i))
                    .withCacheWriter((k, v) -> {
                        try {
                            sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    })
                    .withRemovalListener((key, value, reason) -> {
                        try {
                            sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    }).build();
        }

        CacheManager cacheManager = new CacheManagerImpl();

        long t = System.currentTimeMillis();

        for (int i = 0; i < 1000000; i++) {
            final String cacheName = String.valueOf(i % 10);
            final String key = String.valueOf(i);
            final String value = String.valueOf(i);
            cacheManager.getCache(cacheName).put(key, value);
            String cached = cacheManager.<String, String>getCache(cacheName).get(key);
            if (!value.equals(cached)) {
                fail(String.format("objects not equals: %s != %s", value, cached));
            }
        }

        System.out.println("Test took " + (System.currentTimeMillis() - t) + " milliseconds");
    }
}