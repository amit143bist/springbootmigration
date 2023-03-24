package com.ds.migration.admin.tool;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.ds.migration.admin.tool.client.CoreScheduledBatchLogClient;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogsInformation;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
public class BatchOperationsControllerTests {

	@MockBean
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Before
	public void setup() {

		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	public void test_fetchLastBatchDetails() throws Exception {

		String batchId = UUID.randomUUID().toString();
		mock_coreScheduledBatchLogClient_findLatestBatchByBatchType(batchId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/latestbatch/batchtype/PRONTOMIGRATION")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").isNotEmpty()).andExpect(jsonPath("$.batchId").value(batchId));
	}

	private void mock_coreScheduledBatchLogClient_findLatestBatchByBatchType(String batchId) {

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(batchId);
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		Mockito.when(coreScheduledBatchLogClient.findLatestBatchByBatchType("PRONTOMIGRATION"))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));

	}

	@Test
	public void test_fetchBatchDetailsByBatchId() throws Exception {

		String batchId = UUID.randomUUID().toString();
		mock_coreScheduledBatchLogClient_fetchBatchDetailsByBatchId(batchId);

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/batches/batchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").isNotEmpty()).andExpect(jsonPath("$.batchId").value(batchId))
				.andExpect(jsonPath("$.batchType").value("PRONTOMIGRATION"));
	}

	private void mock_coreScheduledBatchLogClient_fetchBatchDetailsByBatchId(String batchId) {

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(batchId);
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		Mockito.when(coreScheduledBatchLogClient.findBatchByBatchId(batchId))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));

	}

	@Test
	public void test_fetchAllBatchesByBatchType() throws Exception {

		mock_coreScheduledBatchLogClient_fetchAllBatchesByBatchType();

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/tool/allbatches/batchtype/PRONTOMIGRATION")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(status().isOk()).andExpect(jsonPath("$.scheduledBatchLogResponses").isNotEmpty())
				.andExpect(jsonPath("$.scheduledBatchLogResponses").isArray());
	}

	private void mock_coreScheduledBatchLogClient_fetchAllBatchesByBatchType() {

		List<ScheduledBatchLogResponse> scheduledBatchLogResponses = new ArrayList<ScheduledBatchLogResponse>();
		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(UUID.randomUUID().toString());
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		scheduledBatchLogResponses.add(scheduledBatchLogResponse);

		scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(UUID.randomUUID().toString());
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		scheduledBatchLogResponses.add(scheduledBatchLogResponse);

		ScheduledBatchLogsInformation scheduledBatchLogsInformation = new ScheduledBatchLogsInformation();
		scheduledBatchLogsInformation.setScheduledBatchLogResponses(scheduledBatchLogResponses);
		scheduledBatchLogsInformation.setTotalBatchesCount(BigInteger.valueOf(2));

		Mockito.when(coreScheduledBatchLogClient.findAllBatchesByBatchType("PRONTOMIGRATION")).thenReturn(
				new ResponseEntity<ScheduledBatchLogsInformation>(scheduledBatchLogsInformation, HttpStatus.OK));

	}

	@Test
	public void test_closeHungBatch() throws Exception {

		String batchId = UUID.randomUUID().toString();
		mock_coreScheduledBatchLogClient_closeHungBatch(batchId);

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/tool/batches/hungbatchid/" + batchId)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").isNotEmpty()).andExpect(jsonPath("$.batchEndDateTime").isNotEmpty())
				.andExpect(jsonPath("$.batchId").value(batchId))
				.andExpect(jsonPath("$.batchType").value("PRONTOMIGRATION"));
	}

	private void mock_coreScheduledBatchLogClient_closeHungBatch(String batchId) {

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(batchId);
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");
		scheduledBatchLogResponse.setBatchEndDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));

		Mockito.when(coreScheduledBatchLogClient.updateBatch(batchId))
				.thenReturn(new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse, HttpStatus.OK));

	}

	@Test
	public void test_fetcBatchDetailsByDateRange() throws Exception {

		mock_coreScheduledBatchLogClient_fetcBatchDetailsByDateRange();

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/tool/batches/batchtype/prontomigration/fromdate/2019-11-10/todate/2019-11-10")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduledBatchLogResponses").isNotEmpty())
				.andExpect(jsonPath("$.scheduledBatchLogResponses").isArray());
	}

	private void mock_coreScheduledBatchLogClient_fetcBatchDetailsByDateRange() {

		List<ScheduledBatchLogResponse> scheduledBatchLogResponses = new ArrayList<ScheduledBatchLogResponse>();
		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(UUID.randomUUID().toString());
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		scheduledBatchLogResponses.add(scheduledBatchLogResponse);

		scheduledBatchLogResponse = new ScheduledBatchLogResponse();
		scheduledBatchLogResponse.setBatchId(UUID.randomUUID().toString());
		scheduledBatchLogResponse.setBatchStartDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		scheduledBatchLogResponse.setBatchType("PRONTOMIGRATION");

		scheduledBatchLogResponses.add(scheduledBatchLogResponse);

		ScheduledBatchLogsInformation scheduledBatchLogsInformation = new ScheduledBatchLogsInformation();
		scheduledBatchLogsInformation.setScheduledBatchLogResponses(scheduledBatchLogResponses);
		scheduledBatchLogsInformation.setTotalBatchesCount(BigInteger.valueOf(2));

		Mockito.when(coreScheduledBatchLogClient.findAllByBatchTypeAndBatchStartDateTimeBetween("PRONTOMIGRATION",
				"2019-11-10T00:00:00.000", "2019-11-10T23:59:59.999"))
				.thenReturn(new ResponseEntity<ScheduledBatchLogsInformation>(scheduledBatchLogsInformation,
						HttpStatus.OK));

	}

	@Test
	public void test_fetcBatchDetailsByDateRange_1() throws Exception {

		mock_coreScheduledBatchLogClient_fetcBatchDetailsByDateRange();

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/tool/batches/batchtype/prontomigration/fromdate/2019-11-10T00:00:00.000/todate/2019-11-10T23:59:59.999")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduledBatchLogResponses").isNotEmpty())
				.andExpect(jsonPath("$.scheduledBatchLogResponses").isArray())
				.andExpect(jsonPath("$.scheduledBatchLogResponses", hasSize(2)));
	}

	@Test
	public void test_fetcBatchDetailsByDateRange_2() throws Exception {

		mock_coreScheduledBatchLogClient_fetcBatchDetailsByDateRange();

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/tool/batches/batchtype/prontomigration/fromdate/2019-11-10T00:00:00/todate/2019-11-10T23:59:59.999")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

}