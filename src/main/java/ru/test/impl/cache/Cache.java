package ru.test.impl.cache;

import java.util.Date;
import java.util.function.BiConsumer;

/**
 * Интерфейс кэша
 *
 * @param <K> тип ключ по которому хранится объект
 * @param <V> тип хранимого объекта
 */
public interface Cache<K, V> {
    /**
     * Получить объект из кэша по ключу
     *
     * @param key ключ
     * @return объект
     */
    V get(K key);

    /**
     * Полохить объект в кэш
     *
     * @param key   ключ
     * @param value объект
     */
    void put(K key, V value);

    /**
     * Положить объект в кэш с указанием времени хранения
     *
     * @param key        ключ
     * @param value      объект
     * @param expiryDate дата-время до которого хранить объект в кэше
     */
    void put(K key, V value, Date expiryDate);

    /**
     * Удалить объект из кэша
     *
     * @param key ключ
     */
    void remove(K key);

    /**
     * Указать обработчик по сохранению объекта в постоянном хранилище
     *
     * @param cacheWriter обработчик
     */
    void setCacheWriter(BiConsumer<K, V> cacheWriter);

    /**
     * Указать обработчик вызываемый при удалении объекта из кэша
     *
     * @param removalListener обработчик
     */
    void setRemovalListener(RemovalListener<K, V> removalListener);

    interface RemovalListener<K, V> {
        void accept(K key, V value, RemoveReason reason);
    }

    enum RemoveReason {
        MANUAL, // объект удален вручную
        EXPIRED // объект удален по истечению времени хранения
    }
}
