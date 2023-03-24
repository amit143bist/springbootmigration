package com.ds.migration.coredata.transformer;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.ds.migration.common.constant.RetryStatus;
import com.ds.migration.common.exception.InvalidInputException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.coredata.model.CoreProcessFailureLog;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;

@Component
public class CoreProcessFailureLogTransformer {

	public CoreProcessFailureLog transformToCoreProcessFailureLog(
			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogRequest) {

		CoreProcessFailureLog coreProcessFailureLog = new CoreProcessFailureLog();

		Assert.notNull(concurrentProcessFailureLogRequest.getFailureRecordId(), "FailureRecordId cannot be null");

		coreProcessFailureLog.setFailureRecordId(concurrentProcessFailureLogRequest.getFailureRecordId());

		Assert.notNull(concurrentProcessFailureLogRequest.getFailureCode(), "FailureCode cannot be null");
		coreProcessFailureLog.setFailureCode(concurrentProcessFailureLogRequest.getFailureCode());

		Assert.notNull(concurrentProcessFailureLogRequest.getFailureDateTime(), "FailureDateTime cannot be null");
		coreProcessFailureLog.setFailureDateTime(
				MigrationDateTimeUtil.convertToLocalDateTime(concurrentProcessFailureLogRequest.getFailureDateTime()));

		Assert.notNull(concurrentProcessFailureLogRequest.getFailureReason(), "FailureReason cannot be null");
		coreProcessFailureLog.setFailureReason(concurrentProcessFailureLogRequest.getFailureReason());
		coreProcessFailureLog.setProcessId(UUID.fromString(concurrentProcessFailureLogRequest.getProcessId()));

		Assert.notNull(concurrentProcessFailureLogRequest.getFailureStep(), "FailureStep cannot be null");
		coreProcessFailureLog.setFailureStep(concurrentProcessFailureLogRequest.getFailureStep());

		coreProcessFailureLog.setRetryStatus(concurrentProcessFailureLogRequest.getRetryStatus());
		coreProcessFailureLog.setRetryCount(concurrentProcessFailureLogRequest.getRetryCount());

		return coreProcessFailureLog;
	}

	public ConcurrentProcessFailureLogDefinition transformToConcurrentProcessFailureLogResponse(
			CoreProcessFailureLog coreProcessFailureLog) {

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogResponse = new ConcurrentProcessFailureLogDefinition();

		concurrentProcessFailureLogResponse.setProcessFailureId(coreProcessFailureLog.getProcessFailureId().toString());
		concurrentProcessFailureLogResponse.setFailureRecordId(coreProcessFailureLog.getFailureRecordId());
		concurrentProcessFailureLogResponse.setFailureCode(coreProcessFailureLog.getFailureCode());
		concurrentProcessFailureLogResponse
				.setFailureDateTime(MigrationDateTimeUtil.convertToString(coreProcessFailureLog.getFailureDateTime()));
		concurrentProcessFailureLogResponse.setFailureReason(coreProcessFailureLog.getFailureReason());
		concurrentProcessFailureLogResponse.setProcessId(coreProcessFailureLog.getProcessId().toString());
		concurrentProcessFailureLogResponse.setFailureStep(coreProcessFailureLog.getFailureStep());

		Optional.ofNullable(coreProcessFailureLog.getSuccessDateTime()).map(mapper -> {

			concurrentProcessFailureLogResponse.setSuccessDateTime(
					MigrationDateTimeUtil.convertToString(coreProcessFailureLog.getSuccessDateTime()));
			return concurrentProcessFailureLogResponse;
		});

		concurrentProcessFailureLogResponse.setRetryStatus(coreProcessFailureLog.getRetryStatus());
		concurrentProcessFailureLogResponse.setRetryCount(coreProcessFailureLog.getRetryCount());

		return concurrentProcessFailureLogResponse;
	}

	public void updateFailureLogData(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogRequest,
			CoreProcessFailureLog failureLog) {

		failureLog.setRetryStatus(concurrentProcessFailureLogRequest.getRetryStatus());

		if (null != failureLog.getRetryCount()) {

			failureLog.setRetryCount(BigInteger.valueOf(failureLog.getRetryCount().intValue() + 1));
		} else {

			failureLog.setRetryCount(BigInteger.ONE);
		}

		Optional.ofNullable(concurrentProcessFailureLogRequest.getFailureCode()).map(mapper -> {

			failureLog.setFailureCode(concurrentProcessFailureLogRequest.getFailureCode());
			return failureLog;
		});

		Optional.ofNullable(concurrentProcessFailureLogRequest.getFailureReason()).map(mapper -> {

			failureLog.setFailureReason(concurrentProcessFailureLogRequest.getFailureReason());
			return failureLog;
		});

		if (null != concurrentProcessFailureLogRequest.getRetryStatus()
				&& RetryStatus.F.toString().equalsIgnoreCase(concurrentProcessFailureLogRequest.getRetryStatus())) {

			Optional.ofNullable(concurrentProcessFailureLogRequest.getFailureDateTime()).map(mapper -> {

				failureLog.setFailureDateTime(MigrationDateTimeUtil
						.convertToLocalDateTime(concurrentProcessFailureLogRequest.getFailureDateTime()));
				return failureLog;
			}).orElseThrow(() -> new InvalidInputException(
					"FailureDateTime is null for processFailureId# " + failureLog.getProcessFailureId()));

		}

		if (null != concurrentProcessFailureLogRequest.getRetryStatus()
				&& RetryStatus.S.toString().equalsIgnoreCase(concurrentProcessFailureLogRequest.getRetryStatus())) {

			Optional.ofNullable(concurrentProcessFailureLogRequest.getSuccessDateTime()).map(mapper -> {

				failureLog.setSuccessDateTime(MigrationDateTimeUtil
						.convertToLocalDateTime(concurrentProcessFailureLogRequest.getSuccessDateTime()));
				return failureLog;
			}).orElseThrow(() -> new InvalidInputException(
					"SuccessDateTime is null for processFailureId# " + failureLog.getProcessFailureId()));

		}

	}
}