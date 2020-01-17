package ru.test.ejb.rest;

import ru.test.ejb.cache.CacheManagerBean;
import ru.test.impl.cache.Cache;
import ru.test.impl.dto.JmsDto;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

@Path("/jms-cache")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class JmsCacheRestService {

    @Inject
    private Logger log;

    @Inject
    private CacheManagerBean cacheManager;

    @Context
    private UriInfo uriInfo;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private Cache<String, JmsDto> getCache(String cacheName) {
        Cache<String, JmsDto> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return cache;
    }

    /**
     * Найти сообщение по его ID
     *
     * @param cacheName идентификатор кэша
     * @param messageId ID сообщения
     * @return Response
     */
    @GET
    @Path("/{cacheName}/{id}")
    public Response retrieveMessageById(
            @PathParam("cacheName") String cacheName,
            @PathParam("id") String messageId
    ) {
        JmsDto jms = getCache(cacheName).get(messageId);
        if (jms == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return Response.ok(jms).build();
    }

    /**
     * Создать сообщение
     *
     * @param cacheName идентификатор кэша
     * @param jms       сообщение
     * @return Response
     */
    @POST
    @Path("/{cacheName}")
    public Response createMessage(
            @PathParam("cacheName") String cacheName,
            JmsDto jms
    ) {
        if (jms == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder;
        try {
            getCache(cacheName).put(jms.getId(), jms);

            UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
            uriBuilder.path(jms.getId());
            URI location = uriBuilder.build();

            builder = Response.created(location);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.log(SEVERE, "An error occurred while creating message: " + e.getMessage(), e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseObj);
        }

        return builder.build();
    }

    /**
     * Изменить существующее сообщение
     *
     * @param cacheName идентификатор кэша
     * @param messageId ID сообщения
     * @param jms       сообщение
     * @return Response
     */
    @PUT
    @Path("/{cacheName}/{id}")
    public Response updateMessage(
            @PathParam("cacheName") String cacheName,
            @PathParam("id") String messageId,
            @QueryParam("expiryDate") String expiryDateString,
            JmsDto jms
    ) {
        JmsDto existJms = getCache(cacheName).get(messageId);
        if (!messageId.equals(jms.getId())) {
            Response response = Response.status(Response.Status.CONFLICT)
                    .entity("Message ID cannot be changed").build();
            throw new WebApplicationException(response);
        }
        if (existJms == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Response.ResponseBuilder builder;
        try {
            Cache<String, JmsDto> cache = getCache(cacheName);
            if (expiryDateString != null) {
                Date expiryDate = dateFormat.parse(expiryDateString);
                cache.put(jms.getId(), jms, expiryDate);
            } else {
                cache.put(jms.getId(), jms);
            }
            builder = Response.ok(jms);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            Response.Status status;
            if (e instanceof ParseException) {
                status = Response.Status.BAD_REQUEST;
            } else {
                status = Response.Status.INTERNAL_SERVER_ERROR;
                log.log(SEVERE, "An error occurred while updating message (" + messageId + "): " + e.getMessage(), e);
            }
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(status).entity(responseObj);
        }
        return builder.build();
    }

    @DELETE
    @Path("/{cacheName}/{id}")
    public Response deleteMessage(
            @PathParam("cacheName") String cacheName,
            @PathParam("id") String messageId) {
        JmsDto existJms = getCache(cacheName).get(messageId);
        if (existJms == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Response.ResponseBuilder builder;
        try {
            getCache(cacheName).remove(messageId);
            builder = Response.noContent();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.log(SEVERE, "An error occurred while deleting message (" + messageId + "): " + e.getMessage(), e);
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseObj);
        }
        return builder.build();
    }
}
