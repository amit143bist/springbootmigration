package com.ds.migration.auditdata;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.constant.RecordProcessPhaseStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;

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
public class MigrationAuditDataControllerTests extends AbstractTests {

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_InvalidUser() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit").with(httpBasic("migrationuser", "test"))
				.content(asJsonString(new MigrationAuditDataRequest("1234", "84a3a1d3-02e0-4ca5-a5bc-590f37e0834e",
						MigrationDateTimeUtil.convertToString(LocalDateTime.now()),
						RecordProcessPhaseStatus.S.toString(), RecordProcessPhase.FETCH_PRONTO_DOC.toString())))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "TEST")
	public void testMigrationAuditDataController_InvalidRoleUser_1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit")
				.content(asJsonString(new MigrationAuditDataRequest("1234", "84a3a1d3-02e0-4ca5-a5bc-590f37e0834e",
						MigrationDateTimeUtil.convertToString(LocalDateTime.now()),
						RecordProcessPhaseStatus.S.toString(), RecordProcessPhase.FETCH_PRONTO_DOC.toString())))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testMigrationAuditDataController_InvalidUser_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit")
				.content(asJsonString(new MigrationAuditDataRequest("1234", "84a3a1d3-02e0-4ca5-a5bc-590f37e0834e",
						MigrationDateTimeUtil.convertToString(LocalDateTime.now()),
						RecordProcessPhaseStatus.S.toString(), RecordProcessPhase.FETCH_PRONTO_DOC.toString())))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_ValidUser() throws Exception {

		mockMvc.perform(
				MockMvcRequestBuilders.post("/migration/audit").with(httpBasic("migrationuser", "mIgratIonpassword1"))
						.content(asJsonString(new MigrationAuditDataRequest("1234",
								"84a3a1d3-02e0-4ca5-a5bc-590f37e0834e", "2019-09-04T15:17:49.887",
								RecordProcessPhaseStatus.S.toString(), RecordProcessPhase.FETCH_PRONTO_DOC.toString())))
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.auditId").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_ValidAuditRecord() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(new MigrationAuditDataRequest("1234", "84a3a1d3-02e0-4ca5-a5bc-590f37e0834e",
						"2019-09-04T17:17:49.887", RecordProcessPhaseStatus.S.toString(),
						RecordProcessPhase.FETCH_SIGNATURE_DETAILS.toString())))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.auditId").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_ValidAuditRecord_SaveAll() throws Exception {

		String processId = UUID.randomUUID().toString();

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>();
		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest("1234", processId,
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		migrationAuditDataRequest = new MigrationAuditDataRequest("4567", processId,
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
		migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
		migrationAuditDataDefinition.setProcessId(processId);
		migrationAuditDataDefinition.setTotalRecords(BigInteger.valueOf(2));

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit/saveall")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(migrationAuditDataDefinition)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(MigrationAppConstants.SUCCESS_VALUE));

	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_ValidAuditRecord_SaveAllProcedure() throws Exception {

		String processId = UUID.randomUUID().toString();

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>();
		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest("1234", processId,
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		migrationAuditDataRequest = new MigrationAuditDataRequest("4567", processId,
				MigrationDateTimeUtil.convertToString(LocalDateTime.now()), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
		migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
		migrationAuditDataDefinition.setProcessId(processId);
		migrationAuditDataDefinition.setTotalRecords(BigInteger.valueOf(2));

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit/saveallprocedure")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(migrationAuditDataDefinition)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(MigrationAppConstants.SUCCESS_VALUE));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_ValidAuditRecord_Test() throws Exception {

		String processId = UUID.randomUUID().toString();

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>();
		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest("1234",
				UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e").toString(), "2019-09-04T15:17:49.887",
				RecordProcessPhaseStatus.S.toString(), "FETCH_PRONTO_DOC");

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		migrationAuditDataRequest = new MigrationAuditDataRequest("1234",
				UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e").toString(), "2019-09-04T17:17:49.887",
				RecordProcessPhaseStatus.S.toString(), "FETCH_SIGNATURE_DETAILS");

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		migrationAuditDataRequest = new MigrationAuditDataRequest("95875351",
				UUID.fromString("acb4e224-c435-4992-80fb-aff9801e9ad7").toString(), "2019-10-02T18:27:58.060",
				RecordProcessPhaseStatus.S.toString(), "OST_DS_ID_ORACLE");

		migrationAuditDataRequestList.add(migrationAuditDataRequest);

		MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
		migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
		migrationAuditDataDefinition.setProcessId(processId);
		migrationAuditDataDefinition.setTotalRecords(BigInteger.valueOf(2));

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/audit/saveall")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(migrationAuditDataDefinition)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(MigrationAppConstants.SUCCESS_VALUE));

	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_validateAuditEntry_1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/audit/68973656-bef4-483e-90d5-1e275b1131ef/validate")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(MigrationAppConstants.SUCCESS_VALUE));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_validateInvalidAuditEntry_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/audit/05fb0a45-484a-43d4-9694-33edcddbfc68/validate")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$").isNotEmpty()).andExpect(jsonPath("$.message")
						.value("Audit entry not found for auditId# 05fb0a45-484a-43d4-9694-33edcddbfc68"));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_validateRecordEntry_1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/audit/record/1234/validate")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.status").value(MigrationAppConstants.SUCCESS_VALUE));
	}

	@Test
	@WithMockUser(roles = "USER")
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testMigrationAuditDataController_findModifiedRecordId_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/audit/record/1234/validate")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isPreconditionFailed())
				.andExpect(jsonPath("$.status").isNotEmpty()).andExpect(jsonPath("$.status").value("FAILED"))
				.andExpect(jsonPath("$.auditIds").isArray()).andExpect(jsonPath("$.auditIds", hasSize(2)))
				.andExpect(jsonPath("$.auditIds", hasItem("05fb0a45-484a-43d4-9694-33edcddbfc69")))
				.andExpect(jsonPath("$.auditIds", hasItem("68973656-bef4-483e-90d5-1e275b1131ef")));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationAuditDataController_validateInvalidRecordEntry_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/audit/record/99999999/validate")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.message").value("dbAuditEntries is empty or null for inputRecord# 99999999"));
	}

}