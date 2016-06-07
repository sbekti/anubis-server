package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.dao.UserDao;
import io.bekti.anubis.server.util.BCrypt;
import io.bekti.anubis.server.util.ConfigUtils;

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
                           User user) {
        User existingUser = UserDao.getByName(user.getName());

        if (existingUser != null) throw new WebApplicationException();

        int rounds = ConfigUtils.getInteger("bcrypt.rounds");
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(rounds));
        user.setPassword(hashedPassword);

        Integer id = UserDao.add(user);

        if (id != null) {
            URI uri = uriInfo.getAbsolutePathBuilder().path("/" + String.valueOf(id)).build();
            return Response.created(uri).entity(user).build();
        } else {
            throw new WebApplicationException();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("id") int id,
                        @Context UriInfo uriInfo,
                        User user) {

        User existingUser = UserDao.getById(id);

        if (existingUser == null) throw new WebApplicationException();

        int rounds = ConfigUtils.getInteger("bcrypt.rounds");
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(rounds));

        existingUser.setName(user.getName());
        existingUser.setPassword(hashedPassword);

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
