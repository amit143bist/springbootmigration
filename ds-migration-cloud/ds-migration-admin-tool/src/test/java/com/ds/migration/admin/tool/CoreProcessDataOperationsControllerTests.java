package com.ds.migration.admin.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.GetResponse;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
public class CoreProcessDataOperationsControllerTests extends AbstractTests {

	@MockBean
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Value("${migration.processcompletequeue.name}")
	private String processCompleteQueue;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Before
	public void setup() {

		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Before
	public void purgeAllMessages() throws IOException, TimeoutException {

		rabbitAdmin.purgeQueue(processCompleteQueue, true);

	}

	@Test
	public void test_findProcessDetails() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_findProcessByProcessId(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/processes/processid/" + processId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.processId").isNotEmpty()).andExpect(jsonPath("$.processId").value(processId));
	}

	private void mock_findProcessByProcessId(String batchId, String processId) {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessId(processId);
		concurrentProcessLogDefinition
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		Mockito.when(coreConcurrentProcessLogClient.findProcessByProcessId(processId)).thenReturn(
				new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogDefinition, HttpStatus.OK));
	}

	@Test
	public void test_closeHungProcess() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_findProcessByProcessId(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/tool/processes/hungprocessid/" + processId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		Thread.sleep(2000);

		long messageCount = createRabbitMQConnection(processCompleteQueue).messageCount(processCompleteQueue);
		assertThat(messageCount).isEqualTo(1);

		for (int i = 0; i < messageCount; i++) {
			GetResponse getResponse = createRabbitMQConnection(processCompleteQueue).basicGet(processCompleteQueue,
					true);

			String message = new String(getResponse.getBody(), "UTF-8");
			ConcurrentProcessLogDefinition getConcurrentProcessLogDefinition = new ObjectMapper().readValue(message,
					ConcurrentProcessLogDefinition.class);

			assertThat(getConcurrentProcessLogDefinition.getBatchId()).isEqualTo(batchId);
			assertThat(getConcurrentProcessLogDefinition.getProcessId()).isEqualTo(processId);
			assertThat(getConcurrentProcessLogDefinition.getProcessStatus())
					.isEqualTo(ProcessStatus.MANUALLY_CLOSED.toString());
		}
	}

	@Test
	public void test_fetchAllStatusProcesses() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_fetchAllStatusProcesses(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/processes/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCompleteProcess").isNotEmpty())
				.andExpect(jsonPath("$.totalCompleteProcess").value(1))
				.andExpect(jsonPath("$.totalInCompleteProcess").isNotEmpty())
				.andExpect(jsonPath("$.totalInCompleteProcess").value(1))
				.andExpect(jsonPath("$.completeProcessIds").isArray())
				.andExpect(jsonPath("$.inCompleteProcessIds").isArray())
				.andExpect(jsonPath("$.completeProcessIds", hasSize(1)))
				.andExpect(jsonPath("$.inCompleteProcessIds", hasSize(1)));
	}

	@Test
	public void test_fetchInCompleteStatusProcesses() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_fetchAllStatusProcesses(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/incompleteprocesses/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalProcessesCount").isNotEmpty())
				.andExpect(jsonPath("$.totalProcessesCount").value(1));
	}

	@Test
	public void test_fetchCompleteStatusProcesses() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_fetchAllStatusProcesses(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/completeprocesses/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalProcessesCount").isNotEmpty())
				.andExpect(jsonPath("$.totalProcessesCount").value(1));
	}

	@Test
	public void test_calculateCurrentBatchVelocity() throws Exception {

		String batchId = UUID.randomUUID().toString();
		String processId = UUID.randomUUID().toString();
		mock_fetchAllStatusProcesses(batchId, processId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/currentbatchvelocity/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.currentBatchVelocityMap").isNotEmpty());
	}

	private void mock_fetchAllStatusProcesses(String batchId, String processId) {

		List<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitionList = new ArrayList<ConcurrentProcessLogDefinition>();
		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessId(processId);
		concurrentProcessLogDefinition
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.INPROGRESS.toString());

		concurrentProcessLogDefinitionList.add(concurrentProcessLogDefinition);

		ConcurrentProcessLogsInformation inCompleteProcessInformation = new ConcurrentProcessLogsInformation();
		inCompleteProcessInformation.setTotalProcessesCount(BigInteger.valueOf(1));
		inCompleteProcessInformation.setConcurrentProcessLogDefinitions(concurrentProcessLogDefinitionList);

		concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessId(UUID.randomUUID().toString());
		concurrentProcessLogDefinition
				.setProcessStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.COMPLETED.toString());
		concurrentProcessLogDefinition
				.setProcessEndDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		ConcurrentProcessLogsInformation completeProcessInformation = new ConcurrentProcessLogsInformation();
		completeProcessInformation.setTotalProcessesCount(BigInteger.valueOf(1));

		List<ConcurrentProcessLogDefinition> completeConcurrentProcessLogDefinitionList = new ArrayList<ConcurrentProcessLogDefinition>();
		completeConcurrentProcessLogDefinitionList.add(concurrentProcessLogDefinition);
		completeProcessInformation.setConcurrentProcessLogDefinitions(completeConcurrentProcessLogDefinitionList);

		concurrentProcessLogDefinitionList.add(concurrentProcessLogDefinition);

		ConcurrentProcessLogsInformation processInformation = new ConcurrentProcessLogsInformation();
		processInformation.setTotalProcessesCount(BigInteger.valueOf(2));
		processInformation.setConcurrentProcessLogDefinitions(concurrentProcessLogDefinitionList);

		Mockito.when(coreConcurrentProcessLogClient.findAllProcessesForBatchId(batchId))
				.thenReturn(new ResponseEntity<ConcurrentProcessLogsInformation>(processInformation, HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient.findAllInCompleteProcessesForBatchId(batchId)).thenReturn(
				new ResponseEntity<ConcurrentProcessLogsInformation>(inCompleteProcessInformation, HttpStatus.OK));

		Mockito.when(coreConcurrentProcessLogClient.findAllCompleteProcessesForBatchId(batchId)).thenReturn(
				new ResponseEntity<ConcurrentProcessLogsInformation>(completeProcessInformation, HttpStatus.OK));
	}
}