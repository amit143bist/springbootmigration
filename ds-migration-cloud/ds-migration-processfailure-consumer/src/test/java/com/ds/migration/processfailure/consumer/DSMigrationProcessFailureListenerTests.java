package com.ds.migration.processfailure.consumer;

import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = WebEnvironment.NONE)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles(value = "unittest")
@Slf4j
public class DSMigrationProcessFailureListenerTests extends AbstractTests {

	@Value("${migration.queue.ttl}")
	private Integer ttl;// in milliseconds

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Value("${migration.queue.name}")
	private String workerQueueName;

	@Value("${migration.routing.key}")
	private String workerRoutingKey;

	@Value("#{'RETRY_' + '${migration.exchange.name}'}")
	private String retryExchangeName;

	@Value("#{'RETRY_' + '${migration.routing.key}'}")
	private String retryRoutingKey;

	@Value("${migration.exchange.name}")
	private String workerExchangeName;

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("#{'RETRY_' + '${migration.queue.name}'}")
	private String retryQueueName;

	@Before
	public void purgeAllMessages() throws IOException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
		Connection connection;
		connection = factory.newConnection();
		Channel channel = connection.createChannel();

		Queue queue = QueueBuilder.durable(workerQueueName).withArgument("x-dead-letter-exchange", retryExchangeName)
				.withArgument("x-dead-letter-routing-key", retryRoutingKey).build();

		channel.queueDeclare(workerQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(workerQueueName);

		queue = QueueBuilder.durable(retryQueueName).withArgument("x-dead-letter-exchange", workerExchangeName)
				.withArgument("x-dead-letter-routing-key", workerRoutingKey).withArgument("x-message-ttl", ttl).build();

		channel.queueDeclare(retryQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(retryQueueName);

		queue = QueueBuilder.durable(deadQueueName).build();

		channel.queueDeclare(deadQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(deadQueueName);

	}

	@Test
	public void test_CoreProcessFailureLogListener_ProcessMessage_FailedMessage()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition(
				null, UUID.randomUUID().toString(), "ERR-001", "FailureException",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null, "1234",
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), null, null);

		Channel channel = new DSMigrationProcessFailureConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessFailureLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		String message = new String(getResponse.getBody(), "UTF-8");
		ConcurrentProcessFailureLogDefinition getMigrationBatchTriggerInformation = new ObjectMapper()
				.readValue(message, ConcurrentProcessFailureLogDefinition.class);
		assertThat(getMigrationBatchTriggerInformation).isNotNull();
		assertThat(getMigrationBatchTriggerInformation.getProcessId())
				.isEqualTo(concurrentProcessFailureLogDefinition.getProcessId());
		assertThat(getMigrationBatchTriggerInformation.getFailureCode())
				.isEqualTo(concurrentProcessFailureLogDefinition.getFailureCode());

		String errorStatus = getResponse.getProps().getHeaders().get(MQMessageProperties.ERRORSTATUSCODE.toString())
				.toString();

		assertThat(errorStatus).isNotNull();

		String errorTimestamp = getResponse.getProps().getHeaders().get(MQMessageProperties.ERRORTIMESTAMP.toString())
				.toString();

		assertThat(errorTimestamp).isNotNull();

		log.info(
				"Finished DSMigrationProcessFailureConsumerApplicationTests.test_CoreProcessFailureLogListener_ProcessMessage_FailedMessage()");
	}
}