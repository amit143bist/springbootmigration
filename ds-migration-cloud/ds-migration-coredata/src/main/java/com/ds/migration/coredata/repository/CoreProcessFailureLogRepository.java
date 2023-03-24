package com.ds.migration.coredata.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ds.migration.coredata.model.CoreProcessFailureLog;

@Repository(value = "coreProcessFailureLogRepository")
public interface CoreProcessFailureLogRepository extends CrudRepository<CoreProcessFailureLog, UUID> {

	Iterable<CoreProcessFailureLog> findAllByProcessIdAndRetryStatusOrProcessIdAndRetryStatusIsNull(UUID processId,
			String retryStatus, UUID orProcessId);

	Iterable<CoreProcessFailureLog> findAllByFailureRecordIdAndRetryStatusOrFailureRecordIdAndRetryStatusIsNull(
			String failureRecordId, String retryStatus, String orfailureRecordId);

	Iterable<CoreProcessFailureLog> findAllByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(
			List<UUID> processIds, String retryStatus, List<UUID> orProcessIds);

	Long countByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(List<UUID> processIds, String retryStatus,
			List<UUID> orProcessIds);

	Iterable<CoreProcessFailureLog> findAllByRetryStatusOrRetryStatusIsNull(String retryStatus);

	Long countByRetryStatusOrRetryStatusIsNull(String retryStatus);
}