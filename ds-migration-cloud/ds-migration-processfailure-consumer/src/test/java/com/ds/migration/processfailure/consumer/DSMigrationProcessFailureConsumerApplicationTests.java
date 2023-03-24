package com.ds.migration.processfailure.consumer;

import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.processfailure.consumer.client.CoreProcessFailureLogClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class DSMigrationProcessFailureConsumerApplicationTests extends AbstractTests {

	@Value("${migration.queue.ttl}")
	private Integer ttl;// in milliseconds

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Value("${migration.queue.name}")
	private String workerQueueName;

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

	@MockBean
	CoreProcessFailureLogClient coreProcessFailureLogClient;

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

	}

	@Test
	public void test_CoreProcessFailureLogListener_ProcessMessage_SuccessMessage_UpdateCall()
			throws IOException, TimeoutException, InterruptedException {

		String processFailureId = UUID.randomUUID().toString();
		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition(
				processFailureId, UUID.randomUUID().toString(), "ERR-001", "FailureException",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null, "1234",
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), null, null);

		Mockito.when(
				coreProcessFailureLogClient.updateFailureLog(concurrentProcessFailureLogDefinition, processFailureId))
				.thenReturn(new ResponseEntity<ConcurrentProcessFailureLogDefinition>(
						concurrentProcessFailureLogDefinition, HttpStatus.OK));

		Channel channel = new DSMigrationProcessFailureConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessFailureLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

		log.info(
				"Finished DSMigrationProcessFailureConsumerApplicationTests.test_CoreProcessFailureLogListener_ProcessMessage_SuccessMessage_UpdateCall()");
	}

	@Test
	public void test_CoreProcessFailureLogListener_ProcessMessage_FailureMessage_UpdateCall()
			throws IOException, TimeoutException, InterruptedException {

		String processFailureId = UUID.randomUUID().toString();
		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition(
				processFailureId, UUID.randomUUID().toString(), "ERR-001", "FailureException",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null, null,
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), null, null);

		Mockito.when(
				coreProcessFailureLogClient.updateFailureLog(concurrentProcessFailureLogDefinition, processFailureId))
				.thenThrow(new RuntimeException("SQL Error"));

		Channel channel = new DSMigrationProcessFailureConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessFailureLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		String message = new String(getResponse.getBody(), "UTF-8");
		ConcurrentProcessFailureLogDefinition getConcurrentProcessFailureLogDefinition = new ObjectMapper()
				.readValue(message, ConcurrentProcessFailureLogDefinition.class);
		assertThat(getConcurrentProcessFailureLogDefinition).isNotNull();
		assertThat(getConcurrentProcessFailureLogDefinition.getProcessId())
				.isEqualTo(concurrentProcessFailureLogDefinition.getProcessId());
		assertThat(getConcurrentProcessFailureLogDefinition.getFailureCode())
				.isEqualTo(concurrentProcessFailureLogDefinition.getFailureCode());

		log.info(
				"Finished DSMigrationProcessFailureConsumerApplicationTests.test_CoreProcessFailureLogListener_ProcessMessage_FailureMessage_UpdateCall()");
	}

	@Test
	public void test_CoreProcessFailureLogListener_ProcessMessage_SuccessMessage_SaveCall()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition(
				null, UUID.randomUUID().toString(), "ERR-001", "FailureException",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null, "1234",
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), null, null);

		Mockito.when(coreProcessFailureLogClient.saveFailureLog(concurrentProcessFailureLogDefinition)).thenReturn(
				new ResponseEntity<ConcurrentProcessFailureLogDefinition>(concurrentProcessFailureLogDefinition,
						HttpStatus.OK));

		Channel channel = new DSMigrationProcessFailureConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessFailureLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

		log.info(
				"Finished DSMigrationProcessFailureConsumerApplicationTests.test_CoreProcessFailureLogListener_ProcessMessage_SuccessMessage_SaveCall()");
	}

	@Test
	public void test_CoreProcessFailureLogListener_ProcessMessage_FailMessage_SaveCall()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition(
				null, UUID.randomUUID().toString(), "ERR-001", "FailureException",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null, null,
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), null, null);

		Mockito.when(coreProcessFailureLogClient.saveFailureLog(concurrentProcessFailureLogDefinition))
				.thenThrow(new RuntimeException("SQL Error"));

		Channel channel = new DSMigrationProcessFailureConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessFailureLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		String message = new String(getResponse.getBody(), "UTF-8");
		ConcurrentProcessFailureLogDefinition getConcurrentProcessFailureLogDefinition = new ObjectMapper()
				.readValue(message, ConcurrentProcessFailureLogDefinition.class);
		assertThat(getConcurrentProcessFailureLogDefinition).isNotNull();
		assertThat(getConcurrentProcessFailureLogDefinition.getProcessId())
				.isEqualTo(concurrentProcessFailureLogDefinition.getProcessId());
		assertThat(getConcurrentProcessFailureLogDefinition.getFailureCode())
				.isEqualTo(concurrentProcessFailureLogDefinition.getFailureCode());

		log.info(
				"Finished DSMigrationProcessFailureConsumerApplicationTests.test_CoreProcessFailureLogListener_ProcessMessage_SuccessMessage_SaveCall()");
	}

}