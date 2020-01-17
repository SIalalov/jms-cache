package ru.test.ejb.repository;

import ru.test.impl.entity.Jms;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с сущностью Jms
 */
@Stateless
public class JmsRepository {

    @Inject
    private Logger log;

    @Inject
    private EntityManager em;

    /**
     * Поиск сообщения по ID
     *
     * @param id ID сообщения
     * @return Jms
     */
    public Jms findById(String id) {
        return em.find(Jms.class, id);
    }

    /**
     * Создание сообщения
     *
     * @param jms Jms
     * @return Jms
     */
    public Jms create(Jms jms) {
        log.info("Creating message with ID: " + jms.getId());
        em.persist(jms);
        return jms;
    }

    /**
     * Редактирование сообщения
     *
     * @param jms Jms
     * @return Jms
     */
    public Jms update(Jms jms) {
        log.info("Updating message with ID: " + jms.getId());
        em.merge(jms);
        return jms;
    }

    /**
     * Удаление сообщения
     *
     * @param jms Jms
     * @return Jms
     */
    public Jms delete(Jms jms) {
        log.info("Deleting message with ID: " + jms.getId());
        em.remove(jms);
        return jms;
    }
}
