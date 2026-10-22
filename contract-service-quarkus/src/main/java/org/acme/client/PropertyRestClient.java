package org.acme.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.dto.RoomDTO;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;


@RegisterRestClient(configKey = "property-api")
@ApplicationScoped
public interface PropertyRestClient {
    @GET
    @Path("/api/v1/rooms/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    RoomDTO getRoomById(@PathParam("id") Long id);

    @PUT
    @Path("/api/v1/rooms/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Map<String, Object> updateRoomStatus(@PathParam("id") Long id, Map<String, String> statusBody);}
