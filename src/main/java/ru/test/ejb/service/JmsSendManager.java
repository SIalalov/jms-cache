package ru.test.ejb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.test.impl.dto.JmsDto;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Queue;
import java.util.logging.Logger;

@Stateless
public class JmsSendManager {

    @Inject
    private Logger log;

    @Inject
    @JMSConnectionFactory("java:/JmsXA")
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/queue/sample")
    private Queue queue;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void sendMessage(JmsDto jms) {
        log.info("Sending jms message: " + jms);
        try {
            String jmsJson = new ObjectMapper().writeValueAsString(jms);
            jmsContext.createProducer().send(queue, jmsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
