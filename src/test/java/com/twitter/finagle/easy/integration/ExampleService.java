package com.twitter.finagle.easy.integration;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/foo")
public interface ExampleService {

	@GET
	@Path("/bar")
	@Produces(MediaType.APPLICATION_JSON)
	String[] getBar();

	@POST
	@Path("/bar")
	@Consumes(MediaType.APPLICATION_JSON)
	void setBar(String[] bar);

}
