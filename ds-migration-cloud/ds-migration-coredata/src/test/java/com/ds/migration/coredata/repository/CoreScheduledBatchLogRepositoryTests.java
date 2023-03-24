package com.ds.migration.coredata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.util.IterableUtil;
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

import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.coredata.model.CoreScheduledBatchLog;

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
public class CoreScheduledBatchLogRepositoryTests {

	@Autowired
	CoreScheduledBatchLogRepository coreScheduledBatchLogRepository;

	@Test
	public void test_findAllByBatchTypeAndBatchEndDateTimeIsNull() {

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository
				.findAllByBatchTypeAndBatchEndDateTimeIsNull("migrationbatch");

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(1);
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/beforeTestRun.sql", "classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void test_findAllByBatchTypeAndBatchEndDateTimeIsNull_1() {

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository
				.findAllByBatchTypeAndBatchEndDateTimeIsNull("migrationbatch");

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(5);
	}

	@Test
	public void test_findTopByBatchTypeOrderByBatchStartDateTimeDesc() {

		Optional<CoreScheduledBatchLog> scheduledBatchLogOptional = coreScheduledBatchLogRepository
				.findTopByBatchTypeOrderByBatchStartDateTimeDesc("migrationbatch");

		assertThat(scheduledBatchLogOptional).isNotNull();

		scheduledBatchLogOptional.map(scheduledBatchLog -> {

			assertThat(scheduledBatchLog.getBatchId())
					.isEqualTo(UUID.fromString("e781ca58-dec7-44b7-a312-5c21fded402f"));
			return null;
		});

	}

	@Test
	public void test_findAllByBatchTypeAndBatchEndDateTimeIsNotNull() {

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository
				.findAllByBatchTypeAndBatchEndDateTimeIsNotNull("migrationbatch");

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(2);
	}

	@Test
	public void test_findAllByBatchType() {

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository
				.findAllByBatchType("migrationbatch");

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(3);
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void test_findAllByBatchTypeAndBatchStartDateTimeBetween() {

		LocalDateTime fromDate = MigrationDateTimeUtil.convertToLocalDateTime("2019-09-10T00:00:00.000");
		LocalDateTime toDate = MigrationDateTimeUtil.convertToLocalDateTime("2019-09-14T23:59:59.999");

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository
				.findAllByBatchTypeAndBatchStartDateTimeBetween("migrationbatch", fromDate, toDate);

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(2);
	}

	@Test
	public void test_findAll() {

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = coreScheduledBatchLogRepository.findAll();

		assertThat(scheduledBatchLogList).isNotNull();

		assertThat(IterableUtil.sizeOf(scheduledBatchLogList)).isEqualTo(4);
	}

}