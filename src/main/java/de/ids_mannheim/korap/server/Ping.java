package de.ids_mannheim.korap.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A useful ping service.
 *
 * @author Nils Diewald
 */
@Path("ping")
public class Ping {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Gimme 5 minutes, please!";
    };
};
