package com.ds.migration.processcomplete.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.processcomplete.consumer.client.CoreConcurrentProcessLogClient;
import com.ds.migration.processcomplete.consumer.client.CoreScheduledBatchLogClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = WebEnvironment.NONE)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles(value = "unittest")
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorTestProvider")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@SqlGroup({
		@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sqlscripts/beforeTestRun.sql"),
		@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
@Slf4j
public class DSMigrationProcessCompleteConsumerApplicationTests extends AbstractTests {

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
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@MockBean
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Before
	public void purgeAllMessages_Before() throws IOException, TimeoutException {

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

		rabbitAdmin.purgeQueue("RETRY_" + workerQueueName, true);
		rabbitAdmin.purgeQueue("DEAD_" + workerQueueName, true);

	}

	@After
	public void purgeAllMessages_After() throws IOException, TimeoutException {

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

		rabbitAdmin.purgeQueue("RETRY_" + workerQueueName, true);
		rabbitAdmin.purgeQueue("DEAD_" + workerQueueName, true);

	}

	@Test
	public void test_ProcessCompleteListener_ProcessMessage_SuccessMessage_UpdateCall_1()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition(
				"84a3a1d3-02e0-4ca5-a5bc-590f37e0834e", "e781ca58-dec7-44b7-a312-5c21fded402d", "2019-08-27 00:00:00",
				null, "Completed", BigInteger.valueOf(500));

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse(
				concurrentProcessLogDefinition.getBatchId(), "migrationbatch", "2019-08-27 00:00:00",
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), "");
		Mockito.when(coreScheduledBatchLogClient.updateBatch(concurrentProcessLogDefinition.getBatchId()))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient
				.countPendingConcurrentProcessInBatch(concurrentProcessLogDefinition.getBatchId()))
				.thenReturn(new ResponseEntity<Long>(0L, HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient.updateConcurrentProcess(concurrentProcessLogDefinition,
				concurrentProcessLogDefinition.getProcessId()))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogDefinition,
						HttpStatus.OK));

		Channel channel = new DSMigrationProcessCompleteConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);

		log.info("<<<<<<<<<<<<<<<<< Publishing message to the queue >>>>>>>>>>>>");
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessLogDefinition).getBytes());

		Thread.sleep(5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

	}

	@Test
	public void test_ProcessCompleteListener_ProcessMessage_SuccessMessage_UpdateCall_2()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition(
				"84a3a1d3-02e0-4ca5-a5bc-590f37e0834e", "e781ca58-dec7-44b7-a312-5c21fded402d", "2019-08-27 00:00:00",
				null, "Completed", BigInteger.valueOf(500));

		Mockito.when(coreConcurrentProcessLogClient
				.countPendingConcurrentProcessInBatch(concurrentProcessLogDefinition.getBatchId()))
				.thenReturn(new ResponseEntity<Long>(1L, HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient.updateConcurrentProcess(concurrentProcessLogDefinition,
				concurrentProcessLogDefinition.getProcessId()))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogDefinition,
						HttpStatus.OK));

		Channel channel = new DSMigrationProcessCompleteConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);

		log.info("<<<<<<<<<<<<<<<<< Publishing message to the queue >>>>>>>>>>>>");
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessLogDefinition).getBytes());

		Thread.sleep(5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

	}

	@Test
	public void test_ProcessCompleteListener_ProcessMessage_FailureMessage_UpdateCall()
			throws IOException, TimeoutException, InterruptedException {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition(
				"84a3a1d3-02e0-4ca5-a5bc-590f37e0834e", "e781ca58-dec7-44b7-a312-5c21fded402d", "2019-08-27 00:00:00",
				null, "Completed", BigInteger.valueOf(500));

		Mockito.when(coreConcurrentProcessLogClient.updateConcurrentProcess(concurrentProcessLogDefinition,
				concurrentProcessLogDefinition.getProcessId())).thenThrow(new RuntimeException("SQL Error"));

		Channel channel = new DSMigrationProcessCompleteConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);

		log.info("<<<<<<<<<<<<<<<<< Publishing message to the queue >>>>>>>>>>>>");
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessLogDefinition).getBytes());

		Thread.sleep((ttl * (retryLimit + 1)) + 5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNotNull();

		String message = new String(getResponse.getBody(), "UTF-8");
		ConcurrentProcessLogDefinition getConcurrentProcessLogDefinition = new ObjectMapper().readValue(message,
				ConcurrentProcessLogDefinition.class);

		assertThat(getConcurrentProcessLogDefinition).isNotNull();
		assertThat(getConcurrentProcessLogDefinition.getProcessId())
				.isEqualTo(getConcurrentProcessLogDefinition.getProcessId());
		assertThat(getConcurrentProcessLogDefinition.getBatchId())
				.isEqualTo(getConcurrentProcessLogDefinition.getBatchId());

	}
}