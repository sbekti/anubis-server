package io.bekti.anubis.server.model.exception;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public Response toResponse(WebApplicationException e) {
        int statusCode = e.getResponse().getStatus();

        ErrorMessage errorMessage = new ErrorMessage(e.getMessage(), statusCode);

        return Response
                .status(statusCode)
                .entity(errorMessage)
                .build();
    }

}