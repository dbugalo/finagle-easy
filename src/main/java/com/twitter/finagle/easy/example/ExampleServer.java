package com.twitter.finagle.easy.example;

import java.net.InetSocketAddress;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.builder.ServerConfig.Yes;
import com.twitter.finagle.easy.server.ResteasyServiceBuilder;
import com.twitter.finagle.httpx.Http;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.ostrich.admin.AdminHttpService;
import com.twitter.ostrich.admin.RuntimeEnvironment;
import com.twitter.util.Await;

/**
 * Example of creating a server
 *
 * @author ed.peters
 */
public class ExampleServer implements ExampleService {

    @Override
    public String getGreeting(String a) {
        return "Hello, World!" + a;
    }

    public static void main(String [] args) throws Exception {

        Service<Request, Response> service = ResteasyServiceBuilder.get()
        		.withThreadPoolSize(100)
                .withEndpoint(new ExampleServer())
                .build();

        ServerBuilder<Request, Response, Yes, Yes, Yes> builder = ServerBuilder.get()
                .name("ExampleServer")
                .codec(Http.get())
                .bindTo(new InetSocketAddress("localhost", 10000));

        Server server = ServerBuilder.safeBuild(service, builder);

        RuntimeEnvironment runtime = new RuntimeEnvironment("");
        AdminHttpService admin = new AdminHttpService(8000, 0, runtime);
        admin.start();
        
        // from here your application can continue to do other work.
        // the Server object has a background non-daemon thread that
        // will keep the JVM alive until you call close().
        Await.ready(server);
    }

}
