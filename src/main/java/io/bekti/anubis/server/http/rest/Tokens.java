package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.model.dao.Token;
import io.bekti.anubis.server.model.dao.TokenDao;
import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.token.TokenPatch;
import io.bekti.anubis.server.model.token.TokenRequest;
import io.bekti.anubis.server.util.ConfigUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Path("/tokens")
public class Tokens {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<Token> tokens = TokenDao.getAll();

        GenericEntity<List<Token>> entity = new GenericEntity<List<Token>>(tokens) {};

        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("uuid") String uuid) {
        Token token = TokenDao.getByUUID(uuid);

        if (token != null) {
            return Response.ok().entity(token).build();
        } else {
            throw new NotFoundException();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context ContainerRequestContext containerRequestContext,
                           @Context UriInfo uriInfo,
                           TokenRequest tokenRequest) {

        String uuid = UUID.randomUUID().toString();
        String secret = ConfigUtils.getString("access.token.secret");
        String audience = ConfigUtils.getString("access.token.audience");
        long unixTime = System.currentTimeMillis();
        long expiryTime = tokenRequest.getExpiry() * 1000L;

        User user = (User) containerRequestContext.getProperty("user");

        String jwt = Jwts.builder()
                .setId(uuid)
                .setAudience(audience)
                .setSubject(user.getName())
                .setIssuedAt(new Date(unixTime))
                .setExpiration(new Date(unixTime + expiryTime))
                .claim("uid", user.getId())
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();

        Token token = new Token();
        token.setUuid(uuid);
        token.setToken(jwt);
        token.setRevoked(false);

        TokenDao.add(token);

        URI uri = uriInfo.getAbsolutePathBuilder().path("/" + uuid).build();

        return Response.created(uri).entity(token).build();
    }

    @PUT
    @Path("/{uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("uuid") String uuid,
                        TokenPatch tokenPatch) {
        Token token = TokenDao.getByUUID(uuid);

        if (token != null) {
            token.setRevoked(tokenPatch.isRevoked());
            TokenDao.update(token);
            return Response.noContent().build();
        } else {
            throw new NotFoundException();
        }
    }

    @DELETE
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("uuid") String uuid) {
        Token token = TokenDao.getByUUID(uuid);

        if (token != null) {
            TokenDao.delete(token.getId());
            return Response.noContent().build();
        } else {
            throw new NotFoundException();
        }
    }

}
