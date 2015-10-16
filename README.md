# What is this?

[Resteasy](http://www.jboss.org/resteasy) is JBoss's implementation of the 
JAX-RS standard, which in general makes writing REST services a matter of 
slapping some annotations on your code.  Unfortunately, it's built around
the Java Servlet API, so it requires you to run inside a Servlet container.

[Finagle](http://twitter.github.com/finagle/) is an open-source library from 
Twitter that provides an extremely lightweight abstraction for HTTP services,
built on top of [Netty](http://www.jboss.org/netty) (also a JBoss project).

This project bridges the two, allowing you to write REST services as 
Java-annotated classes, and serve them through Finagle to create lightweight
services without having to bundle a web container.

# Tell me more!

Here's an example of a service interface:

```
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/greeting")
public interface ExampleService {

    @GET
    @Produces("application/json")
    String getGreeting();

}
```

Here's a simple server example using this package:

```
import com.opower.finagle.resteasy.server.ResteasyServiceBuilder;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;

import java.net.InetSocketAddress;

public class ExampleServer implements ExampleService {

    @Override
    public String getGreeting() {
        return "Hello, World!";
    }

    public static void main(String [] args) throws Exception {

        Service service = ResteasyServiceBuilder.get()
                .withEndpoint(new ExampleServer())
                .build();

        ServerBuilder builder = ServerBuilder.get()
                .name("ExampleServer")
                .codec(Http.get())
                .bindTo(new InetSocketAddress("localhost", 10000));

        Server server = ServerBuilder.safeBuild(service, builder);

        // ... profit!

    }

}
```

and here's an example of a client for that service:

```
import com.opower.finagle.resteasy.client.ResteasyClientBuilder;

public class ExampleClient {

    public static void main(String [] args) {

        ExampleService service = ResteasyClientBuilder.get()
                .withHttpClient("localhost", 10000)
                .build(ExampleService.class);

        System.out.println(service.getGreeting());

    }

}
```

Notice the supple ease.

# What's left?

There are TODO items remaining in this code, identified with pithy and
insightful comments in the appropriate spots.  The main items are:

* *SSL*: Finagle supports SSL, but it wasn't entirely clear to me how to 
identify at runtime when a Netty request came in on a secure channel.  So 
you should probably be able to create a secure server, but your endpoint
won't necessarily know that it's being invoked securely.  This might get
awkward if you do stuff like generating callback links.

* *Non-blocking Services*: Resteasy supports [asynchronous HTTP request processing](http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/Asynchronous_HTTP_Request_Processing.html)
through some custom annotations and interfaces, for non-blocking service
implementations that want to handle their own threading.  These are not 
supported.

* *Memory*: While processing responses, we have to fully serialize the
response message using Resteasy so we know what headers to send out (since
JAX-RS allows providers to modify headers and body content).  This is
unfortunate; we might be able to do better.



