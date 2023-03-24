package com.ds.migration.processstart.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;
import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataResponse;
import com.ds.migration.feign.prontodata.domain.MigrationSignedDraftDefinition;
import com.ds.migration.feign.prontodata.domain.ProntoDataInformation;
import com.ds.migration.processstart.consumer.client.MigrationAuthenticationClient;
import com.ds.migration.processstart.consumer.client.MigrationProntoDataClient;
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
public class DSMigrationProcessStartConsumerApplicationTests extends AbstractTests {

	@Value("${migration.queue.ttl}")
	private Integer ttl;// in milliseconds

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Value("${migration.queue.name}")
	private String workerQueueName;

	@Value("${migration.failurequeue.name}")
	private String failureQueueName;

	@Value("${migration.auditqueue.name}")
	private String auditQueueName;

	@Value("${migration.prontoqueue.name}")
	private String prontoQueueName;

	@Value("${migration.recordqueue.name}")
	private String recordQueueName;

	@Value("${migration.processcompletequeue.name}")
	private String processCompleteQueueName;

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

	@Value("${migration.prontodummyurl}")
	private String prontoDummyUrl;

	@MockBean
	MigrationProntoDataClient migrationProntoDataClient;

	@MockBean
	MigrationAuthenticationClient migrationAuthenticationClient;

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

		Queue queue = QueueBuilder.durable(workerQueueName).withArgument("x-dead-letter-exchange", retryExchangeName)
				.withArgument("x-dead-letter-routing-key", retryRoutingKey).build();

		channel.queueDeclare(workerQueueName, true, false, false, queue.getArguments());
		channel.queuePurge(workerQueueName);

		rabbitAdmin.purgeQueue("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE", true);

		purgeQueue(failureQueueName, channel);
		purgeQueue(auditQueueName, channel);
		purgeQueue(prontoQueueName, channel);
		purgeQueue(recordQueueName, channel);
		purgeQueue(processCompleteQueueName, channel);

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

		rabbitAdmin.purgeQueue("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE", true);

		purgeQueue(failureQueueName, channel);
		purgeQueue(auditQueueName, channel);
		purgeQueue(prontoQueueName, channel);
		purgeQueue(recordQueueName, channel);
		purgeQueue(processCompleteQueueName, channel);

	}

	private void purgeQueue(String queueName, Channel channel) throws IOException {

		Queue queue = QueueBuilder.durable(queueName).withArgument("x-dead-letter-exchange", retryExchangeName)
				.withArgument("x-dead-letter-routing-key", "RETRY_" + queueName).build();

		channel.queueDeclare(queueName, true, false, false, queue.getArguments());
		channel.queuePurge(queueName);

		rabbitAdmin.purgeQueue("RETRY_" + queueName, true);
		rabbitAdmin.purgeQueue("DEAD_" + queueName, true);
	}

	@Test
	public void test_ProcessStartListener_ProcessMessage_FailedMessage_HoldCall()
			throws IOException, TimeoutException, InterruptedException {

		log.info("started test_ProcessStartListener_ProcessMessage_FailedMessage_HoldCall test");

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition = new ConcurrentProcessMessageDefinition();
		concurrentProcessMessageDefinition.setBatchId(batchId);
		concurrentProcessMessageDefinition.setProcessId(processId);

		mockProntoDataClient_signedDrafts(concurrentProcessMessageDefinition);

		mockAuthenticationService_reqJWTUserToken_Error();

		Channel channel = new DSMigrationProcessStartConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessMessageDefinition).getBytes());

		log.info("Sending thread to sleep");
		Thread.sleep(5000);
		log.info("Thread is active again");

		GetResponse getResponse = createRabbitMQConnection("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE")
				.basicGet("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE", true);

		assertThat(getResponse).isNotNull();

		String message = new String(getResponse.getBody(), "UTF-8");

		ConcurrentProcessMessageDefinition concurrentProcessMessageDefinitionHold = new ObjectMapper()
				.readValue(message, ConcurrentProcessMessageDefinition.class);

		assertThat(concurrentProcessMessageDefinitionHold.getProcessId()).isEqualTo(processId);
	}

	@Test
	public void test_ProcessStartListener_ProcessMessage_SuccessMessage_SaveCall()
			throws IOException, TimeoutException, InterruptedException {

		log.info("started test_ProcessStartListener_ProcessMessage_SuccessMessage_SaveCall test");

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition = new ConcurrentProcessMessageDefinition();
		concurrentProcessMessageDefinition.setBatchId(batchId);
		concurrentProcessMessageDefinition.setProcessId(processId);

		mockProntoDataClient_signedDrafts(concurrentProcessMessageDefinition);

		mockAuthenticationService_reqJWTUserToken();

		mockProntoDataClient_fetchAllSignedDraftDetails(concurrentProcessMessageDefinition.getSignedDraftIds(),
				processId);

		Channel channel = new DSMigrationProcessStartConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessMessageDefinition).getBytes());

		log.info("Sending thread to sleep");
		Thread.sleep((ttl * (retryLimit + 1)) + 20000);
		log.info("Thread is active again");

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(failureQueueName).basicGet(failureQueueName, true);

		assertThat(getResponse).isNull();

		Long messageCount = createRabbitMQConnection(auditQueueName).messageCount(auditQueueName);

		log.info("messageCount is " + messageCount);
		assertThat(messageCount).isNotNull();
		assertThat(messageCount).isEqualTo(3);

		getResponse = createRabbitMQConnection(auditQueueName).basicGet(auditQueueName, true);

		String message = new String(getResponse.getBody(), "UTF-8");

		MigrationAuditDataDefinition migrationAuditDataDefinition = new ObjectMapper().readValue(message,
				MigrationAuditDataDefinition.class);

		log.info("AuditQueue Size is " + migrationAuditDataDefinition.getMigrationAuditDataRequestList().size());

		assertThat(migrationAuditDataDefinition.getMigrationAuditDataRequestList().size()).isNotNull();
		assertThat(migrationAuditDataDefinition.getMigrationAuditDataRequestList().size()).isEqualTo(20);

		migrationAuditDataDefinition.getMigrationAuditDataRequestList().forEach(mapper -> {

			log.info("AuditQueue RecordId " + mapper.getRecordId() + " RecordPhase " + mapper.getRecordPhase()
					+ " Phase Status " + mapper.getRecordPhaseStatus());
		});

		getResponse = createRabbitMQConnection(recordQueueName).basicGet(recordQueueName, true);

		message = new String(getResponse.getBody(), "UTF-8");

		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new ObjectMapper()
				.readValue(message, MigrationRecordIdInformationDefinition.class);

		log.info("RecordQueue Size is "
				+ migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size());

		assertThat(migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size()).isNotNull();
		assertThat(migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size()).isEqualTo(12);

		migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().forEach(mapper -> {

			log.info("RecordQueue RecordId " + mapper.getRecordId() + " DocuSignId " + mapper.getDocuSignId());
		});

		getResponse = createRabbitMQConnection(prontoQueueName).basicGet(prontoQueueName, true);

		message = new String(getResponse.getBody(), "UTF-8");

		migrationRecordIdInformationDefinition = new ObjectMapper().readValue(message,
				MigrationRecordIdInformationDefinition.class);

		log.info("ProntoQueue Size is "
				+ migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size());

		assertThat(migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size()).isNotNull();
		assertThat(migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().size()).isEqualTo(12);

		migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().forEach(mapper -> {

			log.info("ProntoQueue RecordId " + mapper.getRecordId() + " DocuSignId " + mapper.getDocuSignId());
		});

	}

	@Test
	public void test_ProcessStartListener_ProcessMessage_FailedMessage_FailCall()
			throws IOException, TimeoutException, InterruptedException {

		log.info("started test_ProcessStartListener_ProcessMessage_SuccessMessage_SaveCall test");

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition = new ConcurrentProcessMessageDefinition();
		concurrentProcessMessageDefinition.setBatchId(batchId);
		concurrentProcessMessageDefinition.setProcessId(processId);

		mockProntoDataClient_signedDrafts(concurrentProcessMessageDefinition);

		mockAuthenticationService_reqJWTUserToken();

		mockProntoDataClient_fetchAllSignedDraftDetails_OneFailedMessage(
				concurrentProcessMessageDefinition.getSignedDraftIds(), processId);

		Channel channel = new DSMigrationProcessStartConsumerApplicationTests()
				.createRabbitMQConnection(workerQueueName);
		channel.basicPublish(workerExchangeName, workerQueueName, null,
				new ObjectMapper().writeValueAsString(concurrentProcessMessageDefinition).getBytes());

		log.info("Sending thread to sleep");
		Thread.sleep((ttl * (retryLimit + 1)) + 20000);
		log.info("Thread is active again");

		GetResponse getResponse = createRabbitMQConnection(deadQueueName).basicGet(deadQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(workerQueueName).basicGet(workerQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(retryQueueName).basicGet(retryQueueName, true);

		assertThat(getResponse).isNull();

		getResponse = createRabbitMQConnection(failureQueueName).basicGet(failureQueueName, true);

		assertThat(getResponse).isNotNull();

		Long messageCount = createRabbitMQConnection(auditQueueName).messageCount(auditQueueName);

		log.info("messageCount in auditQueueName is " + messageCount);
		assertThat(messageCount).isNotNull();
		assertThat(messageCount).isEqualTo(12);

		messageCount = createRabbitMQConnection(recordQueueName).messageCount(recordQueueName);

		log.info("messageCount in recordQueueName is " + messageCount);
		assertThat(messageCount).isNotNull();
		assertThat(messageCount).isEqualTo(11);

	}

	private void mockProntoDataClient_signedDrafts(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		BigInteger[] signedDraftArr = fetchAllSignedDraftIds(
				MigrationDateTimeUtil.convertToLocalDateTime("2019-09-01T15:00:37.780"),
				MigrationDateTimeUtil.convertToLocalDateTime("2019-09-08T15:00:37.780"));

		List<BigInteger> signedDraftIds = Arrays.asList(signedDraftArr);

		List<BigInteger> signedDraftIdsWithoutDuplicates = signedDraftIds.stream().distinct()
				.collect(Collectors.toList());

		concurrentProcessMessageDefinition.setSignedDraftIds(signedDraftIdsWithoutDuplicates);

		for (BigInteger signedDraftId : signedDraftArr) {

			Mockito.when(migrationProntoDataClient.signedDraft(signedDraftId.longValue()))
					.thenReturn(fetchSignedDraftDetails(signedDraftId.longValue()));
		}
	}

	private void mockProntoDataClient_fetchAllSignedDraftDetails(List<BigInteger> signedDraftIds, String processId) {

		List<Long> signedDraftIdList = signedDraftIds.stream().map(BigInteger::longValue).collect(Collectors.toList());

		MigrationSignedDraftDefinition migrationSignedDraftDefinition = new MigrationSignedDraftDefinition();
		migrationSignedDraftDefinition.setSignedDraftIdList(signedDraftIdList);
		migrationSignedDraftDefinition.setProcessId(processId);

		log.info("signedDraftIds size {}, signedDraftIdsWithoutDuplicates size is {}", signedDraftIds.size(),
				signedDraftIdList.size());

		Map<Long, MigrationProntoDataResponse[]> prontoDataSignedDraftResponseMap = new HashMap<Long, MigrationProntoDataResponse[]>(
				signedDraftIdList.size());

		for (Long signedDraftId : signedDraftIdList) {

			MigrationProntoDataResponse[] prontoSignedDraftData = restTemplate
					.exchange(prontoDummyUrl + "/signeddraftid/{id}", HttpMethod.GET, prepareHTTPEntity(),
							MigrationProntoDataResponse[].class, signedDraftId)
					.getBody();

			prontoDataSignedDraftResponseMap.put(signedDraftId, prontoSignedDraftData);

		}

		ProntoDataInformation prontoDataInformation = new ProntoDataInformation();
		prontoDataInformation.setProntoDataSignedDraftResponseMap(prontoDataSignedDraftResponseMap);
		prontoDataInformation.setDataStatus(ValidationResult.SUCCESS.toString());

		Mockito.when(migrationProntoDataClient.fetchAllSignedDraftDetails(migrationSignedDraftDefinition))
				.thenReturn(new ResponseEntity<ProntoDataInformation>(prontoDataInformation, HttpStatus.OK));
	}

	private void mockProntoDataClient_fetchAllSignedDraftDetails_OneFailedMessage(List<BigInteger> signedDraftIds,
			String processId) {

		List<Long> signedDraftIdList = signedDraftIds.stream().map(BigInteger::longValue).collect(Collectors.toList());

		MigrationSignedDraftDefinition migrationSignedDraftDefinition = new MigrationSignedDraftDefinition();
		migrationSignedDraftDefinition.setSignedDraftIdList(signedDraftIdList);
		migrationSignedDraftDefinition.setProcessId(processId);

		log.info("signedDraftIds size {}, signedDraftIdsWithoutDuplicates size is {}", signedDraftIds.size(),
				signedDraftIdList.size());

		Map<Long, MigrationProntoDataResponse[]> prontoDataSignedDraftResponseMap = new HashMap<Long, MigrationProntoDataResponse[]>(
				signedDraftIdList.size());

		for (Long signedDraftId : signedDraftIdList) {

			if (signedDraftId != 95876081) {
				MigrationProntoDataResponse[] prontoSignedDraftData = restTemplate
						.exchange(prontoDummyUrl + "/signeddraftid/{id}", HttpMethod.GET, prepareHTTPEntity(),
								MigrationProntoDataResponse[].class, signedDraftId)
						.getBody();

				prontoDataSignedDraftResponseMap.put(signedDraftId, prontoSignedDraftData);
			} else {
				prontoDataSignedDraftResponseMap.put(signedDraftId, null);
			}

		}

		ProntoDataInformation prontoDataInformation = new ProntoDataInformation();
		prontoDataInformation.setProntoDataSignedDraftResponseMap(prontoDataSignedDraftResponseMap);
		prontoDataInformation.setDataStatus(ValidationResult.SOMEORALLFAILED.toString());

		Mockito.when(migrationProntoDataClient.fetchAllSignedDraftDetails(migrationSignedDraftDefinition))
				.thenReturn(new ResponseEntity<ProntoDataInformation>(prontoDataInformation, HttpStatus.OK));
	}

	private void mockAuthenticationService_reqJWTUserToken() {

		AuthenticationResponse authenticationResponse = new AuthenticationResponse(null, null,
				"moi+hXBnZJ3r/ZSMShnUJLY5qeU=", "bearer", 3600);
		AuthenticationRequest authenticationRequest = new AuthenticationRequest("fbf8ad6f-9c21-4b9c-a60b-1a6eb04ffffe",
				"signature impersonation");
		Mockito.when(migrationAuthenticationClient.requestJWTUserToken(authenticationRequest))
				.thenReturn(new ResponseEntity<AuthenticationResponse>(authenticationResponse, HttpStatus.CREATED));
	}

	private void mockAuthenticationService_reqJWTUserToken_Error() {

		AuthenticationRequest authenticationRequest = new AuthenticationRequest("8e7f66a5-50c1-498c-8513-c403a35ea3cb",
				"signature impersonation");
		Mockito.when(migrationAuthenticationClient.requestJWTUserToken(authenticationRequest))
				.thenThrow(new RuntimeException("Manually closed"));
	}

	private BigInteger[] fetchAllSignedDraftIds(LocalDateTime beginDateTime, LocalDateTime endDateTime) {

		BigInteger[] signeddraftArr = { BigInteger.valueOf(95876085), BigInteger.valueOf(95876916),
				BigInteger.valueOf(95876507), BigInteger.valueOf(95876385), BigInteger.valueOf(95875351),
				BigInteger.valueOf(95876107), BigInteger.valueOf(95876081), BigInteger.valueOf(95876588),
				BigInteger.valueOf(95877005), BigInteger.valueOf(95876371), BigInteger.valueOf(59694653),
				BigInteger.valueOf(91424063) };
		return signeddraftArr;
	}

	private ResponseEntity<MigrationProntoDataResponse[]> fetchSignedDraftDetails(Long id) {

		return restTemplate.exchange(prontoDummyUrl + "/signeddraftid/{id}", HttpMethod.GET, prepareHTTPEntity(),
				MigrationProntoDataResponse[].class, id);
	}

}