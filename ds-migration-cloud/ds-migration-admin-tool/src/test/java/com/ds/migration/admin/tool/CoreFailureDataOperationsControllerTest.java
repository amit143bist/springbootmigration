package com.ds.migration.admin.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ds.migration.admin.tool.client.CoreConcurrentProcessLogClient;
import com.ds.migration.admin.tool.client.CoreProcessFailureLogClient;
import com.ds.migration.admin.tool.client.CoreScheduledBatchLogClient;
import com.ds.migration.common.constant.RetryStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogsInformation;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.GetResponse;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
public class CoreFailureDataOperationsControllerTest extends AbstractTests {

	@MockBean
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@MockBean
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@MockBean
	CoreProcessFailureLogClient coreProcessFailureLogClient;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Value("${migration.processstartqueue.name}")
	private String processStartQueue;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Before
	public void setup() {

		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Before
	public void purgeAllMessages() throws IOException, TimeoutException {

		rabbitAdmin.purgeQueue(processStartQueue, true);

	}

	@Test
	public void test_listAllFailedMessagesForBatch() throws Exception {

		String batchId = UUID.randomUUID().toString();
		mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/listallfailedmessages/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalFailureCount").isNotEmpty())
				.andExpect(jsonPath("$.totalFailureCount").value(2));
	}

	private String mock_findAllInCompleteProcessesForBatchId(String batchId) {

		List<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitions = new ArrayList<ConcurrentProcessLogDefinition>();
		List<ConcurrentProcessFailureLogDefinition> concurrentProcessFailureLogDefinitions = new ArrayList<ConcurrentProcessFailureLogDefinition>();

		List<ConcurrentProcessFailureLogDefinition> failureConcurrentProcessFailureLogDefinitions = new ArrayList<ConcurrentProcessFailureLogDefinition>();

		String processId = UUID.randomUUID().toString();
		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessId(processId);
		concurrentProcessLogDefinition
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition();
		concurrentProcessFailureLogDefinition.setRetryStatus(RetryStatus.F.toString());
		concurrentProcessFailureLogDefinition.setProcessId(concurrentProcessLogDefinition.getProcessId());
		concurrentProcessFailureLogDefinition.setProcessFailureId(UUID.randomUUID().toString());
		concurrentProcessFailureLogDefinition.setFailureRecordId("1234");

		failureConcurrentProcessFailureLogDefinitions.add(concurrentProcessFailureLogDefinition);
		concurrentProcessFailureLogDefinitions.add(concurrentProcessFailureLogDefinition);

		concurrentProcessLogDefinitions.add(concurrentProcessLogDefinition);

		concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessId(UUID.randomUUID().toString());
		concurrentProcessLogDefinition
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition();
		concurrentProcessFailureLogDefinition.setRetryStatus(RetryStatus.F.toString());
		concurrentProcessFailureLogDefinition.setProcessId(concurrentProcessLogDefinition.getProcessId());
		concurrentProcessFailureLogDefinition.setProcessFailureId(UUID.randomUUID().toString());
		concurrentProcessFailureLogDefinition.setFailureRecordId("6789");

		concurrentProcessFailureLogDefinitions.add(concurrentProcessFailureLogDefinition);

		concurrentProcessLogDefinitions.add(concurrentProcessLogDefinition);

		ConcurrentProcessLogsInformation processInformation = new ConcurrentProcessLogsInformation();
		processInformation.setTotalProcessesCount(BigInteger.valueOf(2));
		processInformation.setConcurrentProcessLogDefinitions(concurrentProcessLogDefinitions);

		Mockito.when(coreConcurrentProcessLogClient.findAllInCompleteProcessesForBatchId(batchId))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogsInformation>(processInformation, HttpStatus.OK));

		List<UUID> processUUIDList = prepareProcessUUIDList(processInformation);

		ConcurrentProcessFailureLogsInformation concurrentProcessFailureLogsInformation = new ConcurrentProcessFailureLogsInformation();
		concurrentProcessFailureLogsInformation.setTotalFailureCount(BigInteger.valueOf(2));
		concurrentProcessFailureLogsInformation
				.setConcurrentProcessFailureLogDefinitions(concurrentProcessFailureLogDefinitions);

		Mockito.when(coreProcessFailureLogClient.listAllProcessFailuresByProcessIds(processUUIDList)).thenReturn(
				new ResponseEntity<ConcurrentProcessFailureLogsInformation>(concurrentProcessFailureLogsInformation,
						HttpStatus.OK));

		concurrentProcessFailureLogsInformation = new ConcurrentProcessFailureLogsInformation();
		concurrentProcessFailureLogsInformation.setTotalFailureCount(BigInteger.valueOf(1));
		concurrentProcessFailureLogsInformation
				.setConcurrentProcessFailureLogDefinitions(failureConcurrentProcessFailureLogDefinitions);

		Mockito.when(coreProcessFailureLogClient.listAllProcessFailureLogForFailureRecordId("1234")).thenReturn(
				new ResponseEntity<ConcurrentProcessFailureLogsInformation>(concurrentProcessFailureLogsInformation,
						HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient
				.findProcessByProcessId(failureConcurrentProcessFailureLogDefinitions.get(0).getProcessId()))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogDefinition,
						HttpStatus.OK));

		Mockito.when(coreProcessFailureLogClient.listAllProcessFailureLogForConcurrentProcessId(
				failureConcurrentProcessFailureLogDefinitions.get(0).getProcessId()))
				.thenReturn(new ResponseEntity<ConcurrentProcessFailureLogsInformation>(
						concurrentProcessFailureLogsInformation, HttpStatus.OK));

		return processId;
	}

	private List<UUID> prepareProcessUUIDList(ConcurrentProcessLogsInformation processInformation) {

		List<UUID> processUUIDList = new ArrayList<UUID>();

		for (ConcurrentProcessLogDefinition concurrentProcessLogDefinition : processInformation
				.getConcurrentProcessLogDefinitions()) {

			processUUIDList.add(UUID.fromString(concurrentProcessLogDefinition.getProcessId()));
		}

		return processUUIDList;
	}

	@Test
	public void test_listAllFailedMessagesForLatestBatch() throws Exception {

		String batchId = UUID.randomUUID().toString();

		mock_listAllFailedMessagesForLatestBatch(batchId);
		mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/tool/listallfailedmessages/latestbatch/batchtype/prontomigration")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalFailureCount").isNotEmpty())
				.andExpect(jsonPath("$.totalFailureCount").value(2));
	}

	private void mock_listAllFailedMessagesForLatestBatch(String batchId) {

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(batchId);
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		Mockito.when(coreScheduledBatchLogClient.findLatestBatchByBatchType("PRONTOMIGRATION"))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));
	}

	@Test
	public void test_retryAllFailedMessagesByBatchId() throws Exception {

		purgeAllMessages();
		String batchId = UUID.randomUUID().toString();

		mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/tool/retryallfailedmessages/batches/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Thread.sleep(2000);

		long messageCount = createRabbitMQConnection(processStartQueue).messageCount(processStartQueue);
		assertThat(messageCount).isEqualTo(2);

		for (int i = 0; i < messageCount; i++) {
			GetResponse getResponse = createRabbitMQConnection(processStartQueue).basicGet(processStartQueue, true);

			String message = new String(getResponse.getBody(), "UTF-8");
			ConcurrentProcessMessageDefinition getConcurrentProcessMessageDefinition = new ObjectMapper()
					.readValue(message, ConcurrentProcessMessageDefinition.class);

			int failedRecordsSize = getConcurrentProcessMessageDefinition.getSignedDraftIds().size();

			assertThat(getConcurrentProcessMessageDefinition.getBatchId()).isEqualTo(batchId);
			assertThat(failedRecordsSize).isEqualTo(1);
			assertThat(!getConcurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty());
		}

	}

	@Test
	public void test_retryAllFailedMessagesForLatestBatch() throws Exception {

		purgeAllMessages();
		String batchId = UUID.randomUUID().toString();

		mock_listAllFailedMessagesForLatestBatch(batchId);
		mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders
				.put("/migration/tool/retryallfailedmessages/latestbatch/batchtype/" + "prontomigration")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Thread.sleep(2000);

		long messageCount = createRabbitMQConnection(processStartQueue).messageCount(processStartQueue);
		assertThat(messageCount).isEqualTo(2);

		for (int i = 0; i < messageCount; i++) {

			GetResponse getResponse = createRabbitMQConnection(processStartQueue).basicGet(processStartQueue, true);

			String message = new String(getResponse.getBody(), "UTF-8");
			ConcurrentProcessMessageDefinition getConcurrentProcessMessageDefinition = new ObjectMapper()
					.readValue(message, ConcurrentProcessMessageDefinition.class);

			int failedRecordsSize = getConcurrentProcessMessageDefinition.getSignedDraftIds().size();

			assertThat(getConcurrentProcessMessageDefinition.getBatchId()).isEqualTo(batchId);
			assertThat(failedRecordsSize).isEqualTo(1);
			assertThat(!getConcurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty());
		}

	}

	@Test
	public void test_retryAllFailedMessagesByFailureRecordId() throws Exception {

		purgeAllMessages();
		String batchId = UUID.randomUUID().toString();

		mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/tool/retryfailedmessages/failures/" + "1234")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Thread.sleep(2000);

		long messageCount = createRabbitMQConnection(processStartQueue).messageCount(processStartQueue);
		assertThat(messageCount).isEqualTo(1);

		for (int i = 0; i < messageCount; i++) {

			GetResponse getResponse = createRabbitMQConnection(processStartQueue).basicGet(processStartQueue, true);

			String message = new String(getResponse.getBody(), "UTF-8");
			ConcurrentProcessMessageDefinition getConcurrentProcessMessageDefinition = new ObjectMapper()
					.readValue(message, ConcurrentProcessMessageDefinition.class);

			int failedRecordsSize = getConcurrentProcessMessageDefinition.getSignedDraftIds().size();

			assertThat(getConcurrentProcessMessageDefinition.getBatchId()).isEqualTo(batchId);
			assertThat(failedRecordsSize).isEqualTo(1);
			assertThat(!getConcurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty());
		}
	}

	@Test
	public void test_retryAllFailedMessagesByProcessId() throws Exception {

		purgeAllMessages();
		String batchId = UUID.randomUUID().toString();
		String processId = mock_findAllInCompleteProcessesForBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/tool/retryfailedmessages/processes/" + processId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Thread.sleep(2000);

		long messageCount = createRabbitMQConnection(processStartQueue).messageCount(processStartQueue);
		assertThat(messageCount).isEqualTo(1);

		for (int i = 0; i < messageCount; i++) {

			GetResponse getResponse = createRabbitMQConnection(processStartQueue).basicGet(processStartQueue, true);

			String message = new String(getResponse.getBody(), "UTF-8");
			ConcurrentProcessMessageDefinition getConcurrentProcessMessageDefinition = new ObjectMapper()
					.readValue(message, ConcurrentProcessMessageDefinition.class);

			int failedRecordsSize = getConcurrentProcessMessageDefinition.getSignedDraftIds().size();

			assertThat(getConcurrentProcessMessageDefinition.getBatchId()).isEqualTo(batchId);
			assertThat(failedRecordsSize).isEqualTo(1);
			assertThat(!getConcurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty());
			assertThat(getConcurrentProcessMessageDefinition.getProcessId()).isEqualTo(processId);
		}
	}

}