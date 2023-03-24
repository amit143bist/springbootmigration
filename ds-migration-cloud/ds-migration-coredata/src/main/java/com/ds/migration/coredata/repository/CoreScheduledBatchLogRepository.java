package com.ds.migration.coredata.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ds.migration.coredata.model.CoreScheduledBatchLog;

@Repository(value = "batchLogRepository")
public interface CoreScheduledBatchLogRepository extends CrudRepository<CoreScheduledBatchLog, UUID> {

	Iterable<CoreScheduledBatchLog> findAllByBatchTypeAndBatchEndDateTimeIsNull(String batchType);

	Iterable<CoreScheduledBatchLog> findAllByBatchTypeAndBatchEndDateTimeIsNotNull(String batchType);

	Optional<CoreScheduledBatchLog> findTopByBatchTypeOrderByBatchStartDateTimeDesc(String batchType);

	Iterable<CoreScheduledBatchLog> findAllByBatchType(String batchType);

	Iterable<CoreScheduledBatchLog> findAllByBatchTypeAndBatchStartDateTimeBetween(String batchType,
			LocalDateTime fromDate, LocalDateTime toDate);
}