package com.ds.migration.coredata.transformer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.coredata.model.CoreScheduledBatchLog;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;

@Component
public class CoreScheduledBatchLogTransformer {

	public CoreScheduledBatchLog transformToCoreScheduledBatchLog(ScheduledBatchLogRequest scheduledBatchLogRequest) {

		CoreScheduledBatchLog coreScheduledBatchLog = new CoreScheduledBatchLog();

		coreScheduledBatchLog.setBatchType(scheduledBatchLogRequest.getBatchType());
		coreScheduledBatchLog.setBatchStartParameters(scheduledBatchLogRequest.getBatchStartParameters());
		coreScheduledBatchLog.setBatchStartDateTime(LocalDateTime.now());
		coreScheduledBatchLog.setTotalRecords(scheduledBatchLogRequest.getTotalRecords());

		return coreScheduledBatchLog;
	}

	public ScheduledBatchLogResponse transformToScheduledBatchLogResponse(CoreScheduledBatchLog coreScheduledBatchLog) {

		ScheduledBatchLogResponse scheduledBatchLogResponse = new ScheduledBatchLogResponse();

		scheduledBatchLogResponse.setBatchId(coreScheduledBatchLog.getBatchId().toString());
		scheduledBatchLogResponse.setBatchStartDateTime(
				MigrationDateTimeUtil.convertToString(coreScheduledBatchLog.getBatchStartDateTime()));
		scheduledBatchLogResponse.setBatchEndDateTime(
				MigrationDateTimeUtil.convertToString(coreScheduledBatchLog.getBatchEndDateTime()));
		scheduledBatchLogResponse.setBatchStartParameters(coreScheduledBatchLog.getBatchStartParameters());
		scheduledBatchLogResponse.setBatchType(coreScheduledBatchLog.getBatchType());

		return scheduledBatchLogResponse;
	}
}