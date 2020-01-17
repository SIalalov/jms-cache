package ru.test.ejb.rest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.test.impl.dto.JmsDto;
import ru.test.impl.entity.Jms;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class JmsCacheRestServiceTest {

    private static final String REST_JMS_CACHE = "rest/jms-cache";

    private static final String ID1 = "id1";
    private static final String ID2 = "id2";
    private static final String ID3 = "id3";

    @Inject
    private EntityManager em;

    @Inject
    @JMSConnectionFactory("java:/JmsXA")
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/queue/sample")
    private Queue queue;

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackages(true, "ru/test")
                .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource("arquillian-ds.xml")
                .addAsManifestResource("activemq-jms.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Проверка корректности создания сообщения
     */
    @Test
    @RunAsClient
    @InSequence(1)
    public void testCreateMessage(@ArquillianResource URL contextPath) {
        String id = ID1;
        JmsDto jmsDto = new JmsDto(id, "first");

        Response response = request(contextPath, "/first")
                .post(Entity.entity(jmsDto, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.CREATED, response.getStatusInfo());
        assertEquals(contextPath + REST_JMS_CACHE + "/first/" + id, response.getLocation().toString());

        response = request(response.getLocation()).get();
        JmsDto jms = response.readEntity(JmsDto.class);
        assertEquals(id, jms.getId());
    }

    /**
     * Проверка изменения сообщения
     * @param contextPath
     */
    @Test
    @RunAsClient
    @InSequence(2)
    public void testUpdateMessage(@ArquillianResource URL contextPath) {
        String id = ID1;
        JmsDto jmsDto = new JmsDto(id, "changed");

        Response response = request(contextPath, "/first/" + id)
                .put(Entity.entity(jmsDto, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK, response.getStatusInfo());

        response = request(contextPath, "/first/" + id).get();
        JmsDto jms = response.readEntity(JmsDto.class);
        assertEquals("changed", jms.getContent());
    }

    /**
     * Проверка обращения к несуществующему кэшу или к несуществующему элементу
     */
    @Test
    @RunAsClient
    @InSequence(3)
    public void testUnknownCache(@ArquillianResource URL contextPath) {
        Response response = request(contextPath, "/lalala/id").get();
        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());

        response = request(contextPath, "/first/id").get();
        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());
    }

    /**
     * Проверка создания, получения, истечения срока
     */
    @Test
    @RunAsClient
    @InSequence(4)
    public void testGetExpired(@ArquillianResource URL contextPath) throws InterruptedException {
        JmsDto jmsDto2 = new JmsDto(ID2, "second");
        Response response = request(contextPath, "/first")
                .post(Entity.entity(jmsDto2, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.CREATED, response.getStatusInfo());
        URI jms1Uri = response.getLocation();

        JmsDto jmsDto3 = new JmsDto(ID3, "third");
        response = request(contextPath, "/second")
                .post(Entity.entity(jmsDto3, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.CREATED, response.getStatusInfo());
        URI jms2Uri = response.getLocation();

        response = request(jms1Uri).get();
        assertEquals(Response.Status.OK, response.getStatusInfo());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        jmsDto2.setContent("changed");
        response = request(contextPath, "/first/" + ID2 + "?expiryDate=wrong_date")
                .put(Entity.entity(jmsDto2, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo());

        response = request(contextPath, "/first/" + ID2 +
                "?expiryDate=" + dateFormat.format(createDate(0)))
                .put(Entity.entity(jmsDto2, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.OK, response.getStatusInfo());

        sleep(2000L);

        response = request(jms1Uri).get();
        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());

        response = request(jms2Uri).get();
        assertEquals(Response.Status.OK, response.getStatusInfo());
    }

    /**
     * Проверка корректности отработки сохранения в персистентном хранилище,
     * отправки сообщения в activemq по истечению срока
     */
    @Test
    @InSequence(5)
    public void testCreatedMessages() {
        List<Jms> list = em.createQuery("select e from Jms e", Jms.class).getResultList();
        assertEquals(2, list.size());
        for (Jms jms : list) {
            switch (jms.getId()) {
                case ID1:
                    assertEquals("changed", jms.getContent());
                    break;
                case ID3:
                    assertEquals("third", jms.getContent());
                    break;
                default: fail("Received unexpected message: " + jms);
            }
        }

        List<Message> receivedMessages = new ArrayList<>();
        JMSConsumer consumer = jmsContext.createConsumer(queue);
        Message message;
        while ((message = consumer.receive(100)) != null) {
            System.out.println(message);
            receivedMessages.add(message);
        }
        assertEquals(1, receivedMessages.size());
    }

    /**
     * Проверка ручного удаления сообщения
     */
    @Test
    @RunAsClient
    @InSequence(6)
    public void testRemoveMessage(@ArquillianResource URL contextPath) {
        Response response = request(contextPath, "/second/" + ID3).get();
        assertEquals(Response.Status.OK, response.getStatusInfo());
        JmsDto jms = response.readEntity(JmsDto.class);
        assertEquals(ID3, jms.getId());

        response = request(contextPath, "/second/" + ID3).delete();
        assertEquals(Response.Status.NO_CONTENT, response.getStatusInfo());

        response = request(contextPath, "/second/" + ID3).get();
        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo());
    }

    /**
     * Проверка, что сообщения удалились из персистентного хранилища, и jms не отправились в activemq
     */
    @Test
    @InSequence(7)
    public void testDeletedMessages() throws InterruptedException {
        sleep(2000L);
        List<Jms> list = em.createQuery("select e from Jms e", Jms.class).getResultList();
        assertEquals(1, list.size());
        Jms jms = list.get(0);
        assertEquals(ID1, jms.getId());

        List<Message> receivedMessages = new ArrayList<>();
        JMSConsumer consumer = jmsContext.createConsumer(queue);
        Message message;
        while ((message = consumer.receive(100)) != null) {
            System.out.println(message);
            receivedMessages.add(message);
        }
        assertEquals(0, receivedMessages.size());
    }

    private Invocation.Builder request(URL contextPath, String relativeUrl) {
        URI uri = UriBuilder.fromUri(contextPath + REST_JMS_CACHE + relativeUrl).port(8080).build();
        return request(uri);
    }

    private Invocation.Builder request(URI uri) {
        return ClientBuilder.newClient().target(uri).request();
    }

    private Date createDate(int nowPlusSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, nowPlusSeconds);
        return calendar.getTime();
    }
}
