package com.ds.migration.prontodata.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataRequest;
import com.ds.migration.feign.prontodata.domain.MigrationSignedDraftDefinition;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
@Slf4j
public class MigrationProntoDataControllerTest extends AbstractTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Before
	public void setup() {

		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	@WithMockUser(roles = "USER")
	public void testMigrationProntodataController_SignedDraftList() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(
				"/migration/prontodata/drafts?beginDateTime=2019-09-01T15:02:10.899&endDateTime=2019-09-08T15:02:10.899")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	public void testMigrationProntodataController_SignedDraft() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/migration/prontodata/signedraft/95876371")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	public void testMigrationProntodataController_fetchAllSignedDraftDetails() throws Exception {

		log.info("StartDateTime in MigrationProntoDataControllerTest is {}", LocalDateTime.now());
		Random rnd = new Random();
		MigrationSignedDraftDefinition migrationSignedDraftDefinition = new MigrationSignedDraftDefinition();
		List<Long> signedDraftIdList = new ArrayList<Long>();

		for (int i = 0; i < 100; i++) {

			signedDraftIdList.add(10000000L + rnd.nextInt(90000000));
		}

		migrationSignedDraftDefinition.setSignedDraftIdList(signedDraftIdList);
		migrationSignedDraftDefinition.setProcessId(UUID.randomUUID().toString());

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/prontodata/allsignedraftdetails")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content(asJsonString(migrationSignedDraftDefinition)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.prontoDataSignedDraftResponseMap").isNotEmpty())
				.andExpect(jsonPath("$.dataStatus").isNotEmpty()).andExpect(jsonPath("$.dataStatus").value("SUCCESS"));

		log.info("EndDateTime in MigrationProntoDataControllerTest is {}", LocalDateTime.now());

	}

	@Test
	public void testMigrationProntodataController_updateSignedDraftWithDocuSignId() throws Exception {
		MigrationProntoDataRequest migrationProntoDataRequest = new MigrationProntoDataRequest();
		migrationProntoDataRequest.setEnvelopeid("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e");

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/prontodata/signedraft/95876371")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content(asJsonString(migrationProntoDataRequest)))
				.andExpect(status().is2xxSuccessful());
	}

	@Test
	public void testMigrationProntodataController_updateAllSignedDraftWithDocuSignId() throws Exception {

		String processId = UUID.randomUUID().toString();
		List<MigrationRecordIdInformation> migrationRecordIdInformationList = new ArrayList<MigrationRecordIdInformation>();
		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinition.setProcessId(processId);

		MigrationRecordIdInformation migrationRecordIdInformation = new MigrationRecordIdInformation();
		migrationRecordIdInformation.setRecordId("1234");
		migrationRecordIdInformation.setDocuSignId(UUID.randomUUID().toString());

		migrationRecordIdInformationList.add(migrationRecordIdInformation);

		migrationRecordIdInformation = new MigrationRecordIdInformation();
		migrationRecordIdInformation.setRecordId("4567");
		migrationRecordIdInformation.setDocuSignId(UUID.randomUUID().toString());
		migrationRecordIdInformationList.add(migrationRecordIdInformation);

		migrationRecordIdInformationDefinition.setMigrationRecordIdInformationList(migrationRecordIdInformationList);
		migrationRecordIdInformationDefinition.setTotalRecords(BigInteger.valueOf(2));

		mockMvc.perform(MockMvcRequestBuilders.put("/migration/prontodata/update/allsignedrafts")
				.with(httpBasic("migrationuser", "mIgratIonpassword1")).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).content(asJsonString(migrationRecordIdInformationDefinition)))
				.andExpect(status().is2xxSuccessful()).andExpect(jsonPath("$.processId").isNotEmpty())
				.andExpect(jsonPath("$.processId").value(processId));
	}
}