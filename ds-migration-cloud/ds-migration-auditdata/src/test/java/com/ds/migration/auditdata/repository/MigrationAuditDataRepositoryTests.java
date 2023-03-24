package com.ds.migration.auditdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ds.migration.auditdata.model.MigrationAuditEntries;
import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.constant.RecordProcessPhaseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles(value = "unittest")
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorTestProvider")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@SqlGroup({
		@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sqlscripts/beforeTestRun.sql"),
		@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
@Slf4j
public class MigrationAuditDataRepositoryTests {

	@Autowired
	private MigrationAuditDataRepository migrationAuditDataRepository;

	@Test
	public void testInsertAuditEntries() throws JsonProcessingException {

		MigrationAuditEntries migrationAuditEntries = new MigrationAuditEntries();
		migrationAuditEntries.setRecordId("12345");

		UUID processUUID = UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e");
		migrationAuditEntries.setProcessId(processUUID);
		migrationAuditEntries.setRecordPhaseStatus(RecordProcessPhaseStatus.S.toString());
		migrationAuditEntries.setRecordPhase(RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString());

		migrationAuditEntries.setAuditEntryDateTime(LocalDateTime.now());
		migrationAuditEntries.setHashedEntry(new ObjectMapper().writeValueAsString(migrationAuditEntries));

		migrationAuditDataRepository.findById(migrationAuditDataRepository.save(migrationAuditEntries).getAuditId())
				.map(entry -> {

					assertThat(entry).isNotNull();
					assertThat(entry.getAuditId()).isNotNull();
					assertThat(entry.getUpdatedBy()).isNull();
					assertThat(entry.getCreatedBy()).isNotNull();

					return entry;
				});
	}

	@Test
	public void testFindAllByRecordId_ValidRecordId() {

		Optional.ofNullable(migrationAuditDataRepository.findAllByRecordId("1234")).map(entries -> {

			assertThat(entries).isNotNull();

			entries.forEach(auditEntry -> {

				assertThat(auditEntry).isNotNull();
				assertThat(auditEntry.getAuditId()).isNotNull();
			});

			return null;
		});
	}

	@Test
	public void testFindAllByRecordId_InvalidRecordId() {

		assertThat(migrationAuditDataRepository.findAllByRecordId("12345")).isEmpty();
	}

	@Test
	public void testFindAllByAuditDataId_ValidAuditDataId_FunctionCall() {

		UUID auditId = UUID.randomUUID();
		Long returnInt = migrationAuditDataRepository.createMigrationAuditData(auditId, "1234", UUID.randomUUID(),
				LocalDateTime.now(), RecordProcessPhaseStatus.S.toString(),
				RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), "abdsdsdsd121212");

		log.info("returnInt {}", returnInt);
		assertThat(migrationAuditDataRepository.findById(auditId)).isNotEmpty();
	}

}