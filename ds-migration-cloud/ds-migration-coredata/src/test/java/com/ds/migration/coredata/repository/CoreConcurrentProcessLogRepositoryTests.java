package com.ds.migration.coredata.repository;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.ds.migration.coredata.model.CoreConcurrentProcessLog;

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
public class CoreConcurrentProcessLogRepositoryTests {

	@Autowired
	CoreConcurrentProcessLogRepository coreConcurrentProcessLogRepository;

	@Test
	public void testCountByBatchIdAndProcessEndDateTime() {

		Long count = coreConcurrentProcessLogRepository
				.countByBatchIdAndProcessEndDateTimeIsNull(UUID.fromString("e781ca58-dec7-44b7-a312-5c21fded402d"));
		assertThat(count).isNotNull();
		assertThat(count).isEqualTo(1);
	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void testfindAllByBatchId() {

		Iterable<CoreConcurrentProcessLog> coreConcurrentProcessLogList = coreConcurrentProcessLogRepository
				.findAllByBatchId(UUID.fromString("b4ad9898-dd2f-43d4-b685-dd08aebc5065"));
		assertThat(coreConcurrentProcessLogList).isNotNull();
		assertThat(IterableUtil.sizeOf(coreConcurrentProcessLogList)).isEqualTo(2);
	}

	@Test
	public void testfindAllByBatchIdAndProcessEndDateTimeIsNull() {

		Iterable<CoreConcurrentProcessLog> coreConcurrentProcessLogList = coreConcurrentProcessLogRepository
				.findAllByBatchIdAndProcessEndDateTimeIsNull(UUID.fromString("e781ca58-dec7-44b7-a312-5c21fded402d"));
		assertThat(coreConcurrentProcessLogList).isNotNull();
		assertThat(IterableUtil.sizeOf(coreConcurrentProcessLogList)).isEqualTo(1);
	}
}