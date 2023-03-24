package com.ds.migration.coredata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
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

import com.ds.migration.coredata.model.CoreProcessFailureLog;

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
public class CoreProcessFailureLogRepositoryTests {

	@Autowired
	CoreProcessFailureLogRepository coreProcessFailureLogRepository;

	@Test
	public void testfindAllByFailureRecordIdAndRetryStatusIsNullOrRetryStatus() {

		Iterable<CoreProcessFailureLog> processFailureLogIter = coreProcessFailureLogRepository
				.findAllByFailureRecordIdAndRetryStatusOrFailureRecordIdAndRetryStatusIsNull("1234", "F", "1234");

		assertThat(IterableUtil.isNullOrEmpty(processFailureLogIter)).isFalse();
		assertThat(IterableUtil.sizeOf(processFailureLogIter)).isEqualTo(2);

	}

	@Test
	public void testfindAllByProcessIdAndRetryStatusOrProcessIdAndRetryStatusIsNull() {

		Iterable<CoreProcessFailureLog> processFailureLogIter = coreProcessFailureLogRepository
				.findAllByProcessIdAndRetryStatusOrProcessIdAndRetryStatusIsNull(
						UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e"), "F",
						UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0834e"));

		assertThat(IterableUtil.isNullOrEmpty(processFailureLogIter)).isFalse();
		assertThat(IterableUtil.sizeOf(processFailureLogIter)).isEqualTo(2);

	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void findAllByProcessIdIn() {

		List<UUID> processIds = new ArrayList<UUID>();
		processIds.add(UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0835e"));
		processIds.add(UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0836e"));

		Iterable<CoreProcessFailureLog> processFailureLogIter = coreProcessFailureLogRepository
				.findAllByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(processIds, "F", processIds);

		assertThat(IterableUtil.isNullOrEmpty(processFailureLogIter)).isFalse();
		assertThat(IterableUtil.sizeOf(processFailureLogIter)).isEqualTo(2);

	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void countByByProcessIdIn() {

		List<UUID> processIds = new ArrayList<UUID>();
		processIds.add(UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0835e"));
		processIds.add(UUID.fromString("84a3a1d3-02e0-4ca5-a5bc-590f37e0836e"));

		Long processFailureLogCount = coreProcessFailureLogRepository
				.countByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(processIds, "F", processIds);

		assertThat(processFailureLogCount).isNotNull();
		assertThat(processFailureLogCount).isEqualTo(2);

	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void findAllByRetryStatusOrRetryStatusIsNull() {

		Iterable<CoreProcessFailureLog> processFailureLogIter = coreProcessFailureLogRepository
				.findAllByRetryStatusOrRetryStatusIsNull("F");

		assertThat(IterableUtil.isNullOrEmpty(processFailureLogIter)).isFalse();
		assertThat(IterableUtil.sizeOf(processFailureLogIter)).isEqualTo(2);

	}

	@Test
	@SqlGroup({
			@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
					"classpath:sqlscripts/preMethodTestRun.sql" }),
			@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sqlscripts/afterTestRun.sql") })
	public void countByRetryStatusOrRetryStatusIsNull() {

		Long processFailureLogCount = coreProcessFailureLogRepository.countByRetryStatusOrRetryStatusIsNull("F");

		assertThat(processFailureLogCount).isNotNull();
		assertThat(processFailureLogCount).isEqualTo(2);

	}

}