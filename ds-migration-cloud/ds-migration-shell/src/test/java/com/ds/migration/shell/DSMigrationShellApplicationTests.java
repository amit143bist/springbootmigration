package com.ds.migration.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.constant.RecordProcessPhaseStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.batch.domain.MigrationBatchTriggerInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT_ENABLED + "=false",
		InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
		"spring.cloud.config.enabled=false" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles(value = "unittest")
@Import(TestApplicationRunner.class)
@Slf4j
public class DSMigrationShellApplicationTests {

	@Autowired
	private Shell shell;

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

		Queue queue = QueueBuilder.durable("CORE_BATCH_TRIGGER_QUEUE")
				.withArgument("x-dead-letter-exchange", "RETRY_MIGRATION_EXCHANGE")
				.withArgument("x-dead-letter-routing-key", "RETRY_CORE_BATCH_TRIGGER_QUEUE").build();

		channel.queueDeclare("CORE_BATCH_TRIGGER_QUEUE", true, false, false, queue.getArguments());
		channel.queuePurge("CORE_BATCH_TRIGGER_QUEUE");

		rabbitAdmin.purgeQueue("CORE_AUDIT_DATA_QUEUE", true);

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

		Queue queue = QueueBuilder.durable("CORE_BATCH_TRIGGER_QUEUE")
				.withArgument("x-dead-letter-exchange", "RETRY_MIGRATION_EXCHANGE")
				.withArgument("x-dead-letter-routing-key", "RETRY_CORE_BATCH_TRIGGER_QUEUE").build();

		channel.queueDeclare("CORE_BATCH_TRIGGER_QUEUE", true, false, false, queue.getArguments());
		channel.queuePurge("CORE_BATCH_TRIGGER_QUEUE");

		rabbitAdmin.purgeQueue("CORE_AUDIT_DATA_QUEUE", true);

	}

	@Test
	public void test_JobCommands_MigrateProntoData1() throws IOException, TimeoutException {

		assertThat(shell.evaluate(() -> "migrateProntoData numberOfHours 10"));

		GetResponse getResponse = createRabbitMQConnection("CORE_BATCH_TRIGGER_QUEUE")
				.basicGet("CORE_BATCH_TRIGGER_QUEUE", true);

		String message = new String(getResponse.getBody(), "UTF-8");
		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new ObjectMapper().readValue(message,
				MigrationBatchTriggerInformation.class);
		assertThat(migrationBatchTriggerInformation).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isNull();
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isNull();
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isEqualTo(10);
		assertThat(migrationBatchTriggerInformation.getNumberOfRecordsPerThread()).isEqualTo(200);
	}

	@Test
	public void test_JobCommands_MigrateProntoData2() throws IOException, TimeoutException {

		// sample DateTime as String is 2019-09-16T11:18:52.764
		LocalDateTime batchStartDateTime = LocalDateTime.now();
		LocalDateTime batchEndDateTime = LocalDateTime.now().plusHours(5);

		log.info("DSMigrationShellApplicationTests.test_JobCommands_MigrateProntoData2() Test"
				+ batchStartDateTime.toString());

		String shellCommand = "migrateProntoData batchStartDateTime " + batchStartDateTime.toString()
				+ " batchEndDateTime " + batchEndDateTime.toString();

		log.info("shell command is {}", shellCommand);
		assertThat(shell.evaluate(() -> shellCommand));

		GetResponse getResponse = createRabbitMQConnection("CORE_BATCH_TRIGGER_QUEUE")
				.basicGet("CORE_BATCH_TRIGGER_QUEUE", true);

		String message = new String(getResponse.getBody(), "UTF-8");
		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new ObjectMapper().readValue(message,
				MigrationBatchTriggerInformation.class);

		assertThat(migrationBatchTriggerInformation).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isEqualTo(batchStartDateTime.toString());
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isNull();
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isEqualTo(batchEndDateTime.toString());
		assertThat(migrationBatchTriggerInformation.getNumberOfRecordsPerThread()).isEqualTo(200);
	}

	@Test
	public void test_JobCommands_MigrateProntoData3() throws IOException, TimeoutException {

		LocalDateTime batchStartDateTime = LocalDateTime.now();
		LocalDateTime batchEndDateTime = LocalDateTime.now().plusHours(5);

		String shellCommand = "migrateProntoData batchStartDateTime " + batchStartDateTime.toString()
				+ " batchEndDateTime " + batchEndDateTime.toString() + " numberOfRecordsPerThread " + 50;

		assertThat(shell.evaluate(() -> shellCommand));

		GetResponse getResponse = createRabbitMQConnection("CORE_BATCH_TRIGGER_QUEUE")
				.basicGet("CORE_BATCH_TRIGGER_QUEUE", true);

		String message = new String(getResponse.getBody(), "UTF-8");
		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new ObjectMapper().readValue(message,
				MigrationBatchTriggerInformation.class);

		assertThat(migrationBatchTriggerInformation).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isEqualTo(batchStartDateTime.toString());
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isNull();
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isEqualTo(batchEndDateTime.toString());
		assertThat(migrationBatchTriggerInformation.getNumberOfRecordsPerThread()).isEqualTo(50);
	}

	@Test
	public void test_JobCommands_MigrateProntoData4() throws IOException, TimeoutException {

		LocalDateTime batchStartDateTime = LocalDateTime.now();

		String shellCommand = "migrateProntoData batchStartDateTime " + batchStartDateTime.toString()
				+ " numberOfRecordsPerThread " + 50 + " numberOfHours " + 10;

		assertThat(shell.evaluate(() -> shellCommand));

		GetResponse getResponse = createRabbitMQConnection("CORE_BATCH_TRIGGER_QUEUE")
				.basicGet("CORE_BATCH_TRIGGER_QUEUE", true);

		String message = new String(getResponse.getBody(), "UTF-8");

		log.info("DSMigrationShellApplicationTests.test_JobCommands_MigrateProntoData4() " + message);

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new ObjectMapper().readValue(message,
				MigrationBatchTriggerInformation.class);

		assertThat(migrationBatchTriggerInformation).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getBatchStartDateTime()).isEqualTo(batchStartDateTime.toString());
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isNotNull();
		assertThat(migrationBatchTriggerInformation.getNumberOfHours()).isEqualTo(10);
		assertThat(migrationBatchTriggerInformation.getBatchEndDateTime()).isNull();
		assertThat(migrationBatchTriggerInformation.getNumberOfRecordsPerThread()).isEqualTo(50);
	}

	@Test
	public void test_JobCommands_ExistingMessage_MigrateProntoData5() throws IOException, TimeoutException {

		purgeAllMessages_Before();
		String processId = UUID.randomUUID().toString();

		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest("1234", processId,
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>();
		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
		migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
		migrationAuditDataDefinition.setProcessId(processId);
		migrationAuditDataDefinition.setTotalRecords(BigInteger.ONE);

		Channel channel = createRabbitMQConnection("CORE_AUDIT_DATA_QUEUE");
		channel.basicPublish("MIGRATION_EXCHANGE", "CORE_AUDIT_DATA_QUEUE", null,
				new ObjectMapper().writeValueAsString(migrationAuditDataDefinition).getBytes());

		LocalDateTime batchStartDateTime = LocalDateTime.now();

		String shellCommand = "migrateProntoData batchStartDateTime " + batchStartDateTime.toString()
				+ " numberOfRecordsPerThread " + 80 + " numberOfHours " + 20;

		assertThat(shell.evaluate(() -> shellCommand));

		GetResponse getResponse = createRabbitMQConnection("CORE_BATCH_TRIGGER_QUEUE")
				.basicGet("CORE_BATCH_TRIGGER_QUEUE", true);

		if (null != getResponse) {

			String message = new String(getResponse.getBody(), "UTF-8");

			log.info("message " + message);

		}
//		assertThat(getResponse).isNull();
	}

	private Channel createRabbitMQConnection(String queueName) throws IOException, TimeoutException {

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