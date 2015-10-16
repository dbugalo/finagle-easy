package com.twitter.finagle.easy.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Sample of a JAX-RS annotated service interface
 *
 * @author ed.peters
 * @author denis.rangel
 */
@Path("/greeting")
public interface ExampleService {

    @GET
    @Produces("application/json")
    String getGreeting(@QueryParam("a") String a);
}
