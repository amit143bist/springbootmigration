package com.ds.migration.batchtrigger.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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
import org.springframework.web.server.ResponseStatusException;

import com.ds.migration.batchtrigger.consumer.client.CoreConcurrentProcessLogClient;
import com.ds.migration.batchtrigger.consumer.client.CoreScheduledBatchLogClient;
import com.ds.migration.batchtrigger.consumer.client.MigrationProntoDataClient;
import com.ds.migration.batchtrigger.consumer.service.BatchDataService;
import com.ds.migration.broker.config.RabbitMQConfiguration;
import com.ds.migration.common.constant.BatchType;
import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.batch.domain.BatchStartParams;
import com.ds.migration.feign.batch.domain.MigrationBatchTriggerInformation;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class DSMigrationBatchTriggerConsumerApplicationTests extends AbstractTests {

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

	@Value("#{'RETRY_' + '${migration.processstartqueue.routing.key}'}")
	private String processRetryRoutingKey;

	@Value("${migration.exchange.name}")
	private String workerExchangeName;

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("#{'RETRY_' + '${migration.queue.name}'}")
	private String retryQueueName;

	@Value("${migration.exchange.name}")
	private String processStartExchangeName;

	@Value("${migration.processstartqueue.name}")
	private String processStartQueueName;

	@Value("${migration.processstartqueue.routing.key}")
	private String processStartRoutingKey;

	@MockBean
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@MockBean
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@MockBean
	MigrationProntoDataClient migrationProntoDataClient;

	@Autowired
	BatchDataService batchDataService;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Before
	public void purgeAllMessages() throws IOException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
		Connection connection;
		connection = factory.newConnection();
		Channel channel = connection.createChannel();

		Queue queue = QueueBuilder.durable(workerQueueName)
				.withArgument(RabbitMQConfiguration.DEAD_LETTER_EXCHANGE_CONSTANT, retryExchangeName)
				.withArgument(RabbitMQConfiguration.DEAD_LETTER_ROUTING_KEY_CONSTANT, retryRoutingKey).build();

		channel.queueDeclare(workerQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(workerQueueName);

		queue = QueueBuilder.durable(processStartQueueName)
				.withArgument(RabbitMQConfiguration.DEAD_LETTER_EXCHANGE_CONSTANT, retryExchangeName)
				.withArgument(RabbitMQConfiguration.DEAD_LETTER_ROUTING_KEY_CONSTANT, processRetryRoutingKey).build();

		channel.queueDeclare(processStartQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(processStartQueueName);

		rabbitAdmin.purgeQueue("CORE_PARALLEL_PROCESS_START_QUEUE", true);
		rabbitAdmin.purgeQueue("RETRY_CORE_PARALLEL_PROCESS_START_QUEUE", true);
		rabbitAdmin.purgeQueue("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE", true);

	}

	@Test
	public void test_BatchTriggerListener_ProcessMessage_SuccessMessage_FirstBatchSaveCall()
			throws IOException, TimeoutException, InterruptedException {

		purgeAllMessages();
		String batchId = UUID.randomUUID().toString();
		String batchStartDateTimeAsStr = "2019-08-25T23:24:47.887";
		String batchEndDateTimeAsStr = "2019-09-26T04:24:47.929";

		BatchStartParams batchStartParams = new BatchStartParams(batchStartDateTimeAsStr, batchEndDateTimeAsStr, 2);

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				BatchType.PRONTOMIGRATION.toString(), batchStartDateTimeAsStr, batchEndDateTimeAsStr, null, 2);

		mockProntoDataClient_SignedDraftList(batchStartDateTimeAsStr, batchEndDateTimeAsStr);
		mockBatchLogClient_saveBatch(batchStartParams, batchId);
		mockProcessLogClient_saveConcurrentProcess(batchStartParams, batchId);
		mockBatchLogClient_findLatestBatchByBatchType(batchEndDateTimeAsStr, migrationBatchTriggerInformation);

		Channel channel = new DSMigrationBatchTriggerConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);

		log.info("<<<<<<<<<<<<<<<<< Publishing message to the queue >>>>>>>>>>>>");
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(migrationBatchTriggerInformation).getBytes());

		Thread.sleep(10000);

		long messageCount = createRabbitMQConnection(processStartQueueName).messageCount(processStartQueueName);

		log.info("message count in the queue is {}", messageCount);

		assertThat(messageCount).isNotNull();
		assertThat(messageCount).isNotNegative();
		assertThat(messageCount).isEqualTo(4);

		GetResponse getResponse = null;
		for (int count = 0; count < 4; count++) {

			getResponse = createRabbitMQConnection(processStartQueueName).basicGet(processStartQueueName, true);

			assertThat(getResponse).isNotNull();
		}

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

	}

	@Test
	public void test_BatchTriggerListener_ProcessMessage_FailureMessage_AnotherBatchRunning()
			throws IOException, TimeoutException, InterruptedException {

		purgeAllMessages();
		String alreadyRunningBatchId = UUID.randomUUID().toString();
		String batchStartDateTimeAsStr = "2019-08-25T23:24:47.887";
		String batchEndDateTimeAsStr = "2019-09-26T04:24:47.929";

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				BatchType.PRONTOMIGRATION.toString(), batchStartDateTimeAsStr, batchEndDateTimeAsStr, null, 2);

		mockBatchLogClient_findLatestBatchByBatchType_AnotherRunningBatch(migrationBatchTriggerInformation,
				alreadyRunningBatchId);

		Channel channel = new DSMigrationBatchTriggerConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);

		log.info("<<<<<<<<<<<<<<<<< Publishing message to the queue >>>>>>>>>>>>");
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(migrationBatchTriggerInformation).getBytes());

		Thread.sleep(10000);

		GetResponse getResponse = createRabbitMQConnection(workerQueueName).basicGet(deadQueueName, true);
		assertThat(getResponse).isNotNull();

		MigrationBatchTriggerInformation migrationBatchTriggerInformationDead = new ObjectMapper()
				.readValue(getResponse.getBody(), MigrationBatchTriggerInformation.class);
		assertThat(migrationBatchTriggerInformationDead.getBatchType())
				.isEqualTo(migrationBatchTriggerInformation.getBatchType());
		assertThat(migrationBatchTriggerInformationDead.getBatchStartDateTime())
				.isEqualTo(migrationBatchTriggerInformation.getBatchStartDateTime());

	}

	@Test
	public void test_BatchTriggerListener_ProcessMessage_SuccessMessage_AnotherBatchCompletedRunning_OnlyHoursTriggerMessage()
			throws IOException, TimeoutException, InterruptedException {

		purgeAllMessages();
		String alreadyRunningBatchId = UUID.randomUUID().toString();

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				BatchType.PRONTOMIGRATION.toString(), null, null, 10, 2);

		ScheduledBatchLogResponse scheduledBatchLogResponse = prepareMockScheduledBatchLogResponse(
				alreadyRunningBatchId);
		BatchStartParams batchStartParams = batchDataService.calculateBatchTriggerParameters(scheduledBatchLogResponse,
				migrationBatchTriggerInformation);

		LocalDateTime newBatchBeginDateTime = MigrationDateTimeUtil.convertToLocalDateTime("2019-09-26T04:24:47.929")
				.plusSeconds(1);
		LocalDateTime newBatchEndDateTime = MigrationDateTimeUtil.convertToLocalDateTime("2019-09-26T04:24:47.929")
				.plusHours(10);
		assertThat(batchStartParams.getBeginDateTime())
				.isEqualTo(MigrationDateTimeUtil.convertToString(newBatchBeginDateTime));
		assertThat(batchStartParams.getEndDateTime())
				.isEqualTo(MigrationDateTimeUtil.convertToString(newBatchEndDateTime));
		assertThat(batchStartParams.getTotalRecordsPerProcess()).isEqualTo(2);

	}

	@Test
	public void test_BatchTriggerListener_ProcessMessage_SuccessMessage_AnotherBatchCompletedRunning_Hours_StartDateTimeTriggerMessage()
			throws IOException, TimeoutException, InterruptedException {

		purgeAllMessages();
		String alreadyRunningBatchId = UUID.randomUUID().toString();

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				BatchType.PRONTOMIGRATION.toString(), "2019-09-22T04:24:47.929", null, 10, 2);

		ScheduledBatchLogResponse scheduledBatchLogResponse = prepareMockScheduledBatchLogResponse(
				alreadyRunningBatchId);
		BatchStartParams batchStartParams = batchDataService.calculateBatchTriggerParameters(scheduledBatchLogResponse,
				migrationBatchTriggerInformation);

		LocalDateTime newBatchEndDateTime = MigrationDateTimeUtil.convertToLocalDateTime("2019-09-22T04:24:47.929")
				.plusHours(10);
		assertThat(batchStartParams.getBeginDateTime()).isEqualTo("2019-09-22T04:24:47.929");
		assertThat(batchStartParams.getEndDateTime())
				.isEqualTo(MigrationDateTimeUtil.convertToString(newBatchEndDateTime));
		assertThat(batchStartParams.getTotalRecordsPerProcess()).isEqualTo(2);

	}

	@Test
	public void test_BatchTriggerListener_ProcessMessage_SuccessMessage_AnotherBatchCompletedRunning_StartDateTime_EndDateTimeTriggerMessage()
			throws IOException, TimeoutException, InterruptedException {

		purgeAllMessages();
		String alreadyRunningBatchId = UUID.randomUUID().toString();

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				BatchType.PRONTOMIGRATION.toString(), "2019-09-22T04:24:47.929", "2019-09-24T04:24:47.929", null, 2);

		ScheduledBatchLogResponse scheduledBatchLogResponse = prepareMockScheduledBatchLogResponse(
				alreadyRunningBatchId);
		BatchStartParams batchStartParams = batchDataService.calculateBatchTriggerParameters(scheduledBatchLogResponse,
				migrationBatchTriggerInformation);

		assertThat(batchStartParams.getBeginDateTime()).isEqualTo("2019-09-22T04:24:47.929");
		assertThat(batchStartParams.getEndDateTime()).isEqualTo("2019-09-24T04:24:47.929");
		assertThat(batchStartParams.getTotalRecordsPerProcess()).isEqualTo(2);

	}

	private ScheduledBatchLogResponse prepareMockScheduledBatchLogResponse(String alreadyRunningBatchId)
			throws JsonProcessingException {

		String batchStartDateTimeAsStr = "2019-08-25T23:24:47.887";
		String batchEndDateTimeAsStr = "2019-09-26T04:24:47.929";

		BatchStartParams batchStartParams = new BatchStartParams(batchStartDateTimeAsStr, batchEndDateTimeAsStr, 2);

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchType(BatchType.PRONTOMIGRATION.toString());
		scheduledBatchLogResponse.setBatchStartDateTime(batchStartDateTimeAsStr);
		scheduledBatchLogResponse.setBatchStartDateTime(batchEndDateTimeAsStr);
		scheduledBatchLogResponse.setBatchStartParameters(new ObjectMapper().writeValueAsString(batchStartParams));
		scheduledBatchLogResponse.setBatchId(alreadyRunningBatchId);
		return scheduledBatchLogResponse;
	}

	private void mockBatchLogClient_findLatestBatchByBatchType_AnotherRunningBatch(
			MigrationBatchTriggerInformation migrationBatchTriggerInformation, String alreadyRunningBatchId)
			throws JsonProcessingException {

		String batchStartDateTimeAsStr = "2019-08-25T23:24:47.887";
		String batchEndDateTimeAsStr = "2019-09-26T04:24:47.929";

		BatchStartParams batchStartParams = new BatchStartParams(batchStartDateTimeAsStr, batchEndDateTimeAsStr, 2);

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchType(BatchType.PRONTOMIGRATION.toString());
		scheduledBatchLogResponse.setBatchStartDateTime(batchStartDateTimeAsStr);
		scheduledBatchLogResponse.setBatchStartParameters(new ObjectMapper().writeValueAsString(batchStartParams));
		scheduledBatchLogResponse.setBatchId(alreadyRunningBatchId);

		Mockito.when(
				coreScheduledBatchLogClient.findLatestBatchByBatchType(migrationBatchTriggerInformation.getBatchType()))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));
	}

	private void mockBatchLogClient_findLatestBatchByBatchType(String batchEndDateTimeAsStr,
			MigrationBatchTriggerInformation migrationBatchTriggerInformation) {
		Mockito.when(
				coreScheduledBatchLogClient.findLatestBatchByBatchType(migrationBatchTriggerInformation.getBatchType()))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, batchEndDateTimeAsStr));
	}

	private void mockProcessLogClient_saveConcurrentProcess(BatchStartParams batchStartParams, String batchId) {
		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.INPROGRESS.toString());
		concurrentProcessLogDefinition
				.setTotalRecordsInProcess(BigInteger.valueOf(batchStartParams.getTotalRecordsPerProcess()));

		Mockito.when(coreConcurrentProcessLogClient.saveConcurrentProcess(concurrentProcessLogDefinition))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(
						mockConcurrentProcessLogDefinitionResponse(2, batchId), HttpStatus.OK))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(
						mockConcurrentProcessLogDefinitionResponse(2, batchId), HttpStatus.OK))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(
						mockConcurrentProcessLogDefinitionResponse(2, batchId), HttpStatus.OK))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(
						mockConcurrentProcessLogDefinitionResponse(1, batchId), HttpStatus.OK));

		concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.INPROGRESS.toString());
		concurrentProcessLogDefinition.setTotalRecordsInProcess(BigInteger.valueOf(1));

		Mockito.when(coreConcurrentProcessLogClient.saveConcurrentProcess(concurrentProcessLogDefinition))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(
						mockConcurrentProcessLogDefinitionResponse(1, batchId), HttpStatus.OK));

	}

	private ConcurrentProcessLogDefinition mockConcurrentProcessLogDefinitionResponse(Integer totalRecordsPerThread,
			String batchId) {
		ConcurrentProcessLogDefinition concurrentProcessLogDefinitionResponse = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinitionResponse.setBatchId(batchId);
		concurrentProcessLogDefinitionResponse.setProcessStatus(ProcessStatus.INPROGRESS.toString());
		concurrentProcessLogDefinitionResponse.setTotalRecordsInProcess(BigInteger.valueOf(totalRecordsPerThread));
		concurrentProcessLogDefinitionResponse.setProcessId(UUID.randomUUID().toString());
		concurrentProcessLogDefinitionResponse
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		return concurrentProcessLogDefinitionResponse;
	}

	private void mockBatchLogClient_saveBatch(BatchStartParams batchStartParams, String batchId)
			throws JsonProcessingException {

		LocalDateTime batchStartDateTime = LocalDateTime.now();

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse(batchId,
				BatchType.PRONTOMIGRATION.toString(), batchStartDateTime.toString(), null,
				new ObjectMapper().writeValueAsString(batchStartParams));

		ScheduledBatchLogRequest scheduledBatchLogRequest = new ScheduledBatchLogRequest();

		scheduledBatchLogRequest.setBatchType(BatchType.PRONTOMIGRATION.toString());
		scheduledBatchLogRequest.setBatchStartParameters(new ObjectMapper().writeValueAsString(batchStartParams));
		scheduledBatchLogRequest.setTotalRecords(BigInteger.valueOf(7));

		Mockito.when(coreScheduledBatchLogClient.saveBatch(scheduledBatchLogRequest))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));
	}

	private void mockProntoDataClient_SignedDraftList(String batchStartDateTimeAsStr, String batchEndDateTimeAsStr) {
		BigInteger[] signedDraftIds = new BigInteger[] { BigInteger.valueOf(1), BigInteger.valueOf(2),
				BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5), BigInteger.valueOf(6),
				BigInteger.valueOf(7) };

		Mockito.when(migrationProntoDataClient.signedDraftList(
				MigrationDateTimeUtil.convertToLocalDateTime(batchStartDateTimeAsStr),
				MigrationDateTimeUtil.convertToLocalDateTime(batchEndDateTimeAsStr)))
				.thenReturn(new ResponseEntity<BigInteger[]>(signedDraftIds, HttpStatus.OK));
	}

}