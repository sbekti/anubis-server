package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.http.AnubisHttpServer;
import io.bekti.anubis.server.model.client.ClientInfo;
import io.bekti.anubis.server.worker.ClientThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;

@Path("/sessions")
public class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<ClientInfo> clients = new ArrayList<>();

        AnubisHttpServer.getClients().forEach((k, v) -> clients.add(v.getClientInfo()));

        GenericEntity<List<ClientInfo>> entity = new GenericEntity<List<ClientInfo>>(clients) {};

        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("uuid") String uuid) {
        ClientInfo result = null;

        for (ClientThread clientThread : AnubisHttpServer.getClients().values()) {
            ClientInfo clientInfo = clientThread.getClientInfo();

            if (clientInfo.getToken().getUuid().equals(uuid)) {
                result = clientInfo;
                break;
            }
        }

        if (result != null) {
            return Response.ok().entity(result).build();
        } else {
            throw new NotFoundException();
        }
    }

    @DELETE
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("uuid") String uuid) {
        boolean found = false;

        for (ClientThread clientThread : AnubisHttpServer.getClients().values()) {
            ClientInfo clientInfo = clientThread.getClientInfo();

            if (clientInfo.getToken().getUuid().equals(uuid)) {
                try {
                    clientThread.getSession().close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                found = true;
            }
        }

        if (found) {
            return Response.noContent().build();
        } else {
            throw new NotFoundException();
        }
    }

}
