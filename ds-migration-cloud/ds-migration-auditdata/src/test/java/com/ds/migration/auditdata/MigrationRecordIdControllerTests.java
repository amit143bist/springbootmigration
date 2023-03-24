package com.ds.migration.auditdata;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
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
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;

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
public class MigrationRecordIdControllerTests extends AbstractTests {

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationRecordIdController_saveRecordIdData() throws Exception {

		mockMvc.perform(
				MockMvcRequestBuilders.post("/migration/records").with(httpBasic("migrationuser", "mIgratIonpassword1"))
						.content(asJsonString(new MigrationRecordIdInformation("1234", UUID.randomUUID().toString())))
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value("1234"));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationRecordIdController_saveAllRecordIdData() throws Exception {

		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();

		List<MigrationRecordIdInformation> migrationRecordIdInformationList = new ArrayList<MigrationRecordIdInformation>();

		migrationRecordIdInformationList.add(new MigrationRecordIdInformation("1234", UUID.randomUUID().toString()));
		migrationRecordIdInformationList.add(new MigrationRecordIdInformation("4567", UUID.randomUUID().toString()));

		migrationRecordIdInformationDefinition.setMigrationRecordIdInformationList(migrationRecordIdInformationList);
		migrationRecordIdInformationDefinition.setProcessId(UUID.randomUUID().toString());
		migrationRecordIdInformationDefinition
				.setTotalRecords(BigInteger.valueOf(migrationRecordIdInformationList.size()));

		mockMvc.perform(MockMvcRequestBuilders.post("/migration/records/saveall")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(migrationRecordIdInformationDefinition)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").value(MigrationAppConstants.SUCCESS_VALUE));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationRecordIdController_checkInvalidDocuSignIdError() throws Exception {

		mockMvc.perform(
				MockMvcRequestBuilders.post("/migration/records").with(httpBasic("migrationuser", "mIgratIonpassword1"))
						.content(asJsonString(new MigrationRecordIdInformation("1234", "test")))
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.message").value("Invalid UUID string: test"));
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationRecordIdController_findRecordId_1() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/records/9876")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.recordId").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationRecordIdController_findInvalidRecordId_2() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/migration/records/54321")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}
}