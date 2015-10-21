package com.twitter.finagle.easy.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.curator.test.TestingServer;
import org.junit.Test;

import com.twitter.finagle.Httpx;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.easy.client.ClientBuilder;
import com.twitter.finagle.easy.server.ServiceBuilder;
import com.twitter.finagle.httpx.Request;
import com.twitter.finagle.httpx.Response;
import com.twitter.util.Duration;

/**
 * Creates a client and a server and sends data back and forth.
 *
 * @author ed.peters
 * @author denis.rangel
 */
public class TestExhaustiveClientAndServer {

	/*
	 * First port number to start checking when trying to open up a server for
	 * testing
	 */
	public static final int BASE_PORT = 10000;

	/**
	 * Maximum number of attempts to create a server before the test will be
	 * considered a failure
	 */
	public static final int MAX_TRIES = 10;

	@Test
	public void performExhaustiveTest() throws Exception {

		ExampleServiceImpl impl = new ExampleServiceImpl();
		Service<Request, Response> service = ServiceBuilder.get().withEndpoint(impl).build();

		ListeningServer server = createServer(service);
		assertNotNull("couldn't allocate server", server);

		ClientBuilder builder = ClientBuilder.get().withService("localhost:" + BASE_PORT);
		ExampleService client = builder.build(ExampleService.class);

		for (int i = 0; i < MAX_TRIES; i++) {
			String[] expectedValue = new String[] { UUID.randomUUID().toString(), UUID.randomUUID().toString() };
			client.setBar(expectedValue);
			assertArrayEquals("wrong value passed", expectedValue, impl.getBar());
			assertArrayEquals("wrong value returned", expectedValue, client.getBar());
		}

		builder.close();
		server.close(Duration.zero());
	}

	@Test
	public void performExhaustiveZookeeperTest() throws Exception {
		TestingServer zkServer = new TestingServer(2182);

		ExampleServiceImpl impl = new ExampleServiceImpl();
		Service<Request, Response> service = ServiceBuilder.get().withEndpoint(impl).build();

		ListeningServer server = createServer(service);
		server.announce("zk!localhost:2182!/zktest!0");
		assertNotNull("couldn't allocate server", server);

		Thread.sleep(5000);
		ClientBuilder builder = ClientBuilder.get().withService("zk!localhost:2182!/zktest");
		ExampleService client = builder.build(ExampleService.class);
		
		for (int i = 0; i < MAX_TRIES; i++) {
			String[] expectedValue = new String[] { UUID.randomUUID().toString(), UUID.randomUUID().toString() };
			client.setBar(expectedValue);
			assertArrayEquals("wrong value passed", expectedValue, impl.getBar());
			assertArrayEquals("wrong value returned", expectedValue, client.getBar());
		}

		builder.close();
		server.close(Duration.zero());
		zkServer.close();
	}

	/*
	 * Opens up a new server on an available port by starting at the BASE_PORT
	 * and going up from there. If this doesn't succeed after MAX_TRIES, throws
	 * an exception.
	 */
	protected static ListeningServer createServer(Service<Request, Response> service) {
		return Httpx.serve(":" + BASE_PORT, service);
	}
}
