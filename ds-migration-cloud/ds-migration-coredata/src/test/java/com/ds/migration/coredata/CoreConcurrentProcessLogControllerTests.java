package com.ds.migration.coredata;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles(value = "unittest")
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorTestProvider")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@SqlGroup({
		@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sqlscripts/beforeTestRun.sql"),
		@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
@AutoConfigureMockMvc
public class CoreConcurrentProcessLogControllerTests extends AbstractTests {

	@Test
	public void testCoreConcurrentProcessLogController_inValidUser_saveConcurrentProcess1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/scheduledbatch/concurrentprocess")
				.with(httpBasic("migrationuser", "invalidPassword")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "INVALIDROLE")
	public void testCoreConcurrentProcessLogController_inValidUser_saveConcurrentProcess2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/scheduledbatch/concurrentprocess")
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testCoreConcurrentProcessLogController_validUser_saveConcurrentProcess1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/scheduledbatch/concurrentprocess")
				.content(asJsonString(new ConcurrentProcessLogDefinition(null, "e781ca58-dec7-44b7-a312-5c21fded402f",
						MigrationDateTimeUtil.convertToString(LocalDateTime.now()), null,
						ProcessStatus.INPROGRESS.toString(), BigInteger.valueOf(40))))
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.processId").isNotEmpty());
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_validUser_updateConcurrentProcess() throws Exception {

		ConcurrentProcessLogDefinition concurrentProcessLogRequest = new ConcurrentProcessLogDefinition();
		concurrentProcessLogRequest.setProcessStatus(ProcessStatus.COMPLETED.toString());
		mockMvc.perform(MockMvcRequestBuilders
				.put("/migration/scheduledbatch/concurrentprocess/processes/84a3a1d3-02e0-4ca5-a5bc-590f37e0835e")
				.content(asJsonString(concurrentProcessLogRequest))
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.processEndDateTime").isNotEmpty());
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_validUser_countPendingConcurrentProcessInBatch()
			throws Exception {

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/scheduledbatch/concurrentprocess/countprocesses/b4ad9898-dd2f-43d4-b685-dd08aebc5065")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(2));
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_inValidBatchId_countPendingConcurrentProcessInBatch()
			throws Exception {

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/scheduledbatch/concurrentprocess/countprocesses/84a3a1d3-02e0-4ca5-a5bc-590f37e0835e")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(0));
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_findAllProcessesForBatchId() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/scheduledbatch/concurrentprocess/processes/b4ad9898-dd2f-43d4-b685-dd08aebc5065")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isNotEmpty())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isArray())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions", hasSize(2)));
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_findAllInCompleteProcessesForBatchId() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/scheduledbatch/concurrentprocess/incompleteprocesses/b4ad9898-dd2f-43d4-b685-dd08aebc5065")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isNotEmpty())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isArray())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions", hasSize(2)));
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_findAllCompleteProcessesForBatchId() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/scheduledbatch/concurrentprocess/completeprocesses/e781ca58-dec7-44b7-a312-5c21fded402d")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}
	
	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_findAllCompleteProcessesForBatchId_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/scheduledbatch/concurrentprocess/completeprocesses/b4ad9898-dd2f-43d4-b685-dd08aebc5067")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isNotEmpty())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions").isArray())
				.andExpect(jsonPath("$.concurrentProcessLogDefinitions", hasSize(1)));
	}
	
	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testCoreConcurrentProcessLogController_findProcessByProcessId() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders
				.get("/migration/scheduledbatch/concurrentprocess/process/84a3a1d3-02e0-4ca5-a5bc-590f37e0836e")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.batchId").isNotEmpty())
				.andExpect(jsonPath("$.batchId").value("b4ad9898-dd2f-43d4-b685-dd08aebc5065"));
	}
}