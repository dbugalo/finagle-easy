package com.twitter.finagle.easy.example;

import com.twitter.finagle.Httpx;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.easy.server.ServiceBuilder;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.ostrich.admin.AdminHttpService;
import com.twitter.ostrich.admin.RuntimeEnvironment;
import com.twitter.util.Await;

/**
 * Example of creating a server
 *
 * @author denis.rangel
 */
public class ExampleServer implements ExampleService {

	@Override
	public String getGreeting(String a) {
		return "Hello, World!" + a;
	}

	public static void main(String[] args) throws Exception {

		Service<Request, Response> service = ServiceBuilder.get().withThreadPoolSize(100)
				.withEndpoint(new ExampleServer()).build();

		ListeningServer server = Httpx.serve(":10000", service);

		RuntimeEnvironment runtime = new RuntimeEnvironment("");
		AdminHttpService admin = new AdminHttpService(8000, 0, runtime);
		admin.start();
		
		Await.ready(server);
	}

}
