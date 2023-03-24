package com.ds.migration.prontodata.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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

import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.prontodata.consumer.client.MigrationProntoDataClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = WebEnvironment.NONE)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles(value = "unittest")
@Slf4j
public class DSMigrationProntoDataConsumerApplicationTests extends AbstractTests {

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
	MigrationProntoDataClient migrationProntoDataClient;

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

		log.info("Purged all messages");
	}

	@Test
	public void test_ProntoDataListener_ProcessMessage_SuccessMessage_UpdateCall()
			throws IOException, TimeoutException, InterruptedException {

		String docuSignId = UUID.randomUUID().toString();
		MigrationRecordIdInformation migrationRecordIdInformation = new MigrationRecordIdInformation("1234",
				docuSignId);

		List<MigrationRecordIdInformation> migrationRecordIdInformationList = new ArrayList<MigrationRecordIdInformation>();
		migrationRecordIdInformationList.add(migrationRecordIdInformation);

		String processId = UUID.randomUUID().toString();
		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinition.setMigrationRecordIdInformationList(migrationRecordIdInformationList);
		migrationRecordIdInformationDefinition.setProcessId(processId);
		migrationRecordIdInformationDefinition.setTotalRecords(BigInteger.ONE);

		List<MigrationRecordIdInformation> migrationRecordIdInformationListResp = new ArrayList<MigrationRecordIdInformation>();
		migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinition
				.setMigrationRecordIdInformationList(migrationRecordIdInformationListResp);
		migrationRecordIdInformationDefinition.setProcessId(processId);
		migrationRecordIdInformationDefinition.setTotalRecords(BigInteger.valueOf(0));

		Mockito.when(
				migrationProntoDataClient.updateAllSignedDraftWithDocuSignId(migrationRecordIdInformationDefinition))
				.thenReturn(new ResponseEntity<MigrationRecordIdInformationDefinition>(
						migrationRecordIdInformationDefinition, HttpStatus.OK));

		Channel channel = new DSMigrationProntoDataConsumerApplicationTests().createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(migrationRecordIdInformationDefinition).getBytes());

		Thread.sleep(5000);

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();
	}

}