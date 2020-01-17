package ru.test.ejb.service;

import ru.test.ejb.repository.JmsRepository;
import ru.test.impl.cache.Cache;
import ru.test.impl.dto.JmsDto;
import ru.test.impl.entity.Jms;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static ru.test.impl.cache.Cache.RemoveReason.EXPIRED;

/**
 * Сервис для контроля взаимодействия с сущностью Jms в соответствии ECB pattern
 */
@Stateless
public class JmsService {

    @Inject
    private JmsRepository repository;

    @Inject
    private JmsSendManager jmsSendManager;

    public Jms findById(String id) {
        return repository.findById(id);
    }

    public Jms create(Jms jms) {
        return repository.create(jms);
    }

    public Jms update(Jms jms) {
        return repository.update(jms);
    }

    public Jms delete(Jms jms, Cache.RemoveReason reason) {
        if (EXPIRED.equals(reason)) {
            jmsSendManager.sendMessage(JmsDto.from(jms));
        }
        jms = findById(jms.getId());
        if (jms != null) {
            return repository.delete(jms);
        }
        return null;
    }
}
