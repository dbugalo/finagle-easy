package com.twitter.finagle.easy.integration;

public class ExampleServiceImpl implements ExampleService {

	private String[] bar;

	@Override
	public String[] getBar() {
		return this.bar;
	}

	@Override
	public void setBar(String[] bar) {
		this.bar = bar;
	}

}
