package com.twitter.finagle.easy.example;

import com.twitter.finagle.easy.client.ClientBuilder;

/**
 * Example of creating a client
 *
 * @author denis.rangel
 */
public final class ExampleClient {

	private ExampleClient() {
	}

	public static void main(String[] args) throws Exception {

		ClientBuilder clientBuilder = ClientBuilder.get().withHttpClient("localhost", 10000);
		ExampleService service = clientBuilder.build(ExampleService.class);

		System.out.println(service.getGreeting("gfgfgfg"));
		
		clientBuilder.close();
	}

}
