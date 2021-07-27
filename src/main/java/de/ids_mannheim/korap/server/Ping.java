package de.ids_mannheim.korap.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A useful ping service.
 * 
 * @author Nils Diewald
 */
@Path("ping")
public class Ping {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt () {
        return "Gimme 5 minutes, please!";
    };
};
