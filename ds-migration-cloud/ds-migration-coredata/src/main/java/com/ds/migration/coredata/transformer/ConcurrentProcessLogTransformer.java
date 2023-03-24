package com.ds.migration.coredata.transformer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.coredata.model.CoreConcurrentProcessLog;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;

@Component
public class ConcurrentProcessLogTransformer {

	public CoreConcurrentProcessLog tranformToCoreConcurrentProcessLog(
			ConcurrentProcessLogDefinition concurrentProcessLogRequest) {

		CoreConcurrentProcessLog coreConcurrentProcessLog = new CoreConcurrentProcessLog();
		coreConcurrentProcessLog.setBatchId(UUID.fromString(concurrentProcessLogRequest.getBatchId()));
		coreConcurrentProcessLog.setProcessStatus(concurrentProcessLogRequest.getProcessStatus());
		coreConcurrentProcessLog.setTotalRecordsInProcess(concurrentProcessLogRequest.getTotalRecordsInProcess());
		coreConcurrentProcessLog.setProcessStartDateTime(LocalDateTime.now());

		return coreConcurrentProcessLog;

	}

	public ConcurrentProcessLogDefinition tranformToConcurrentProcessLogResponse(
			CoreConcurrentProcessLog coreConcurrentProcessLog) {

		ConcurrentProcessLogDefinition concurrentProcessLogResponse = new ConcurrentProcessLogDefinition();
		concurrentProcessLogResponse.setBatchId(coreConcurrentProcessLog.getBatchId().toString());
		concurrentProcessLogResponse.setProcessId(coreConcurrentProcessLog.getProcessId().toString());
		concurrentProcessLogResponse.setProcessStartDateTime(
				MigrationDateTimeUtil.convertToString(coreConcurrentProcessLog.getProcessStartDateTime()));
		concurrentProcessLogResponse.setProcessEndDateTime(
				MigrationDateTimeUtil.convertToString(coreConcurrentProcessLog.getProcessEndDateTime()));
		concurrentProcessLogResponse.setProcessStatus(coreConcurrentProcessLog.getProcessStatus());
		concurrentProcessLogResponse.setTotalRecordsInProcess(coreConcurrentProcessLog.getTotalRecordsInProcess());

		return concurrentProcessLogResponse;
	}

}
