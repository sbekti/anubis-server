package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.http.annotation.PATCH;
import io.bekti.anubis.server.model.dao.Token;
import io.bekti.anubis.server.model.dao.TokenDao;
import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.token.TokenPatchRequest;
import io.bekti.anubis.server.model.token.TokenCreateRequest;
import io.bekti.anubis.server.util.ConfigUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
                           @NotNull @Valid TokenCreateRequest tokenCreateRequest) {

        String uuid = UUID.randomUUID().toString();
        String secret = ConfigUtils.getString("access.token.secret");
        String audience = ConfigUtils.getString("access.token.audience");
        long unixTime = System.currentTimeMillis();
        long expiryTime = tokenCreateRequest.getExpiry() * 1000L;

        User user = (User) containerRequestContext.getProperty("user");

        String jwt = Jwts.builder()
                .setId(uuid)
                .setAudience(audience)
                .setSubject(user.getName())
                .setIssuedAt(new Date(unixTime))
                .setExpiration(new Date(unixTime + expiryTime))
                .claim("uid", user.getId())
                .claim("name", tokenCreateRequest.getName())
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();

        Token token = new Token();
        token.setUuid(uuid);
        token.setName(tokenCreateRequest.getName());
        token.setToken(jwt);
        token.setRevoked(false);

        Integer id = TokenDao.add(token);

        if (id != null) {
            URI uri = uriInfo.getAbsolutePathBuilder().path("/" + uuid).build();
            return Response.created(uri).entity(token).build();
        } else {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PATCH
    @Path("/{uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patch(@PathParam("uuid") String uuid,
                          @NotNull @Valid TokenPatchRequest tokenPatchRequest) {
        Token token = TokenDao.getByUUID(uuid);

        if (token != null) {

            if (tokenPatchRequest.isRevokedSet()) {
                token.setRevoked(tokenPatchRequest.isRevoked());
            }

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
