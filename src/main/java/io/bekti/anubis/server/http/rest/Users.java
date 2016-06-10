package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.http.annotation.PATCH;
import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.dao.UserDao;
import io.bekti.anubis.server.model.user.UserCreateRequest;
import io.bekti.anubis.server.model.user.UserPatchRequest;
import io.bekti.anubis.server.model.user.UserPutRequest;
import io.bekti.anubis.server.util.BCrypt;
import io.bekti.anubis.server.util.ConfigUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/users")
public class Users {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<User> users = UserDao.getAll();
        GenericEntity<List<User>> entity = new GenericEntity<List<User>>(users) {};

        return Response.ok().entity(entity).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") int id) {
        User user = UserDao.getById(id);

        if (user != null) {
            return Response.ok().entity(user).build();
        } else {
            throw new NotFoundException();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context UriInfo uriInfo,
                           @NotNull @Valid UserCreateRequest userCreateRequest) {
        User existingUser = UserDao.getByName(userCreateRequest.getName());

        if (existingUser != null) {
            throw new WebApplicationException("Name is already taken.", Response.Status.BAD_REQUEST);
        }

        int rounds = ConfigUtils.getInteger("bcrypt.rounds");
        String hashedPassword = BCrypt.hashpw(userCreateRequest.getPassword(), BCrypt.gensalt(rounds));

        User user = new User();
        user.setName(userCreateRequest.getName());
        user.setPassword(hashedPassword);

        Integer id = UserDao.add(user);

        if (id != null) {
            URI uri = uriInfo.getAbsolutePathBuilder().path("/" + String.valueOf(id)).build();
            return Response.created(uri).entity(user).build();
        } else {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("id") int id,
                        @Context UriInfo uriInfo,
                        @NotNull @Valid UserPutRequest userPutRequest) {

        User existingUser = UserDao.getById(id);

        if (existingUser == null) throw new NotFoundException();

        existingUser.setName(userPutRequest.getName());

        int rounds = ConfigUtils.getInteger("bcrypt.rounds");
        String hashedPassword = BCrypt.hashpw(userPutRequest.getPassword(), BCrypt.gensalt(rounds));
        existingUser.setPassword(hashedPassword);

        UserDao.update(existingUser);

        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patch(@PathParam("id") int id,
                          @Context UriInfo uriInfo,
                          @NotNull @Valid UserPatchRequest userPatchRequest) {

        User existingUser = UserDao.getById(id);

        if (existingUser == null) throw new NotFoundException();

        if (userPatchRequest.isNameSet()) {
            existingUser.setName(userPatchRequest.getName());
        }

        if (userPatchRequest.isPasswordSet()) {
            int rounds = ConfigUtils.getInteger("bcrypt.rounds");
            String hashedPassword = BCrypt.hashpw(userPatchRequest.getPassword(), BCrypt.gensalt(rounds));
            existingUser.setPassword(hashedPassword);
        }

        UserDao.update(existingUser);

        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") int id) {
        User user = UserDao.getById(id);

        if (user != null) {
            UserDao.delete(user.getId());
            return Response.noContent().build();
        } else {
            throw new NotFoundException();
        }
    }

}
