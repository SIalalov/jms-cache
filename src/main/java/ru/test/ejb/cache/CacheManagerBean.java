package ru.test.ejb.cache;

import ru.test.ejb.service.JmsService;
import ru.test.impl.cache.Cache;
import ru.test.impl.cache.CacheBuilder;
import ru.test.impl.cache.CacheManagerImpl;
import ru.test.impl.dto.JmsDto;
import ru.test.impl.entity.Jms;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Startup
@Singleton
public class CacheManagerBean extends CacheManagerImpl {

    @Inject
    private JmsService jmsService;

    @PostConstruct
    private void postConstruct() {
        createCache();
        startMaintenance();
    }

    @Schedule(second = "*", minute = "*", hour = "*", info = "scheduleRemoveExpired")
    private void scheduleRemoveExpired() {
        removeExpiredItems();
    }

    private void createCache() {
        CacheBuilder.<String, JmsDto>create("first")
                .withCacheWriter(this::persistObject)
                .withRemovalListener(this::removeObject)
                .build();
        CacheBuilder.<String, JmsDto>create("second")
                .withCacheWriter(this::persistObject)
                .withRemovalListener(this::removeObject)
                .build();
    }

    private void persistObject(String key, JmsDto jmsDto) {
        Jms jms = jmsService.findById(jmsDto.getId());
        if (jms == null) {
            jms = JmsDto.to(jmsDto, new Jms());
            jmsService.create(jms);
        } else {
            JmsDto.to(jmsDto, jms);
            jmsService.update(jms);
        }
    }

    private void removeObject(String key, JmsDto jmsDto, Cache.RemoveReason reason) {
        Jms jms = JmsDto.to(jmsDto, new Jms());
        jmsService.delete(jms, reason);
    }
}
