package ru.test.impl.cache;

/**
 * Интерфейс по взаимодействию с кэшами
 */
public interface CacheManager {
    /**
     * Получить сконфигрурированный кэш по имени
     *
     * @param name имя кэша
     * @param <K>  тип ключ по которому хранится объект
     * @param <V>  тип хранимого объекта
     * @return кэш
     */
    <K, V> Cache<K, V> getCache(String name);
}
