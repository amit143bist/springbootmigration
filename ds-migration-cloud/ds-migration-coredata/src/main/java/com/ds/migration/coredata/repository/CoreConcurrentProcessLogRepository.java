package com.ds.migration.coredata.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ds.migration.coredata.model.CoreConcurrentProcessLog;

@Repository(value = "concurrentProcessLogRepository")
public interface CoreConcurrentProcessLogRepository extends CrudRepository<CoreConcurrentProcessLog, UUID> {

	Long countByBatchIdAndProcessEndDateTimeIsNull(UUID batchId);

	Iterable<CoreConcurrentProcessLog> findAllByBatchId(UUID batchId);

	Iterable<CoreConcurrentProcessLog> findAllByBatchIdAndProcessEndDateTimeIsNull(UUID batchId);

	Iterable<CoreConcurrentProcessLog> findAllByBatchIdAndProcessEndDateTimeIsNotNull(UUID batchId);
}