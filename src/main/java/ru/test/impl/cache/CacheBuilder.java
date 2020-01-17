package ru.test.impl.cache;

import java.util.function.BiConsumer;

/**
 * Вспомогательный класс по конфигурированию кэша
 *
 * @param <K> тип ключа по которому хранится объект
 * @param <V> тип хранимого объекта
 */
public class CacheBuilder<K, V> {

    private String cacheName;

    private CacheImpl<K, V> cache;

    private CacheBuilder() {
    }

    /**
     * Задание имени и типов ключа и хранимого объекта
     *
     * @param cacheName имя
     * @param <K>       тип ключа
     * @param <V>       тип хранимого объекта
     * @return конструктор
     */
    public static <K, V> CacheBuilder<K, V> create(String cacheName) {
        CacheBuilder<K, V> builder = new CacheBuilder<>();
        builder.cache = new CacheImpl<>();
        builder.cacheName = cacheName;
        return builder;
    }

    /**
     * Указать обработчик по сохранению объекта в постоянном хранилище
     *
     * @param cacheWriter обработчик
     * @return конструктор
     */
    public CacheBuilder<K, V> withCacheWriter(BiConsumer<K, V> cacheWriter) {
        cache.setCacheWriter(cacheWriter);
        return this;
    }

    /**
     * Указать обработчик вызываемый при удалении объекта из кэша
     *
     * @param removalListener обработчик
     */
    public CacheBuilder<K, V> withRemovalListener(Cache.RemovalListener<K, V> removalListener) {
        cache.setRemovalListener(removalListener);
        return this;
    }

    /**
     * Завержить создание и вернуть кэш
     *
     * @return кэш
     */
    public Cache<K, V> build() {
        CacheManagerImpl.setCache(cacheName, cache);
        return cache;
    }
}
