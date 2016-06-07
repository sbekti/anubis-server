package io.bekti.anubis.server.http.rest;

import io.bekti.anubis.server.model.dao.User;
import io.bekti.anubis.server.model.dao.UserDao;
import io.bekti.anubis.server.util.BCrypt;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean authorized = true;

        MultivaluedMap<String, String> headers = requestContext.getHeaders();

        String userName = headers.getFirst("x-anubis-username");
        String password = headers.getFirst("x-anubis-password");

        if (userName == null || password == null) {
            authorized = false;
        }

        User user = null;

        if (authorized) {
            user = UserDao.getByName(userName);

            if (user == null) {
                authorized = false;
            } else {
                if (!BCrypt.checkpw(password, user.getPassword())) {
                    authorized = false;
                }
            }
        }

        if (!authorized) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity("")
                    .build()
            );
        } else {
            requestContext.setProperty("user", user);
        }
    }

}
