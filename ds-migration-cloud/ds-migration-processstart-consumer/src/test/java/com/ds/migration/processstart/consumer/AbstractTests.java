package com.ds.migration.processstart.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public abstract class AbstractTests {

	RestTemplate restTemplate = new RestTemplate();

	protected HttpEntity<String> prepareHTTPEntity() {

		HttpHeaders headers = new HttpHeaders();

		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "bearer" + " " + "dummytoken");

		return new HttpEntity<>(headers);
	}

	protected Channel createRabbitMQConnection(String queueName) throws IOException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
		Connection connection;
		connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclarePassive(queueName);

		return channel;
	}
}