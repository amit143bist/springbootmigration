package com.ds.migration.coredata.validator;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.validator.IMigrationValidator;

@Service
public class FailureLogValidator implements IMigrationValidator<ConcurrentProcessFailureLogDefinition> {

	@Override
	public void validateSaveData(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogRequest) {

		Assert.notNull(concurrentProcessFailureLogRequest.getProcessId(), "ProcessId cannot be null");
		Assert.notNull(concurrentProcessFailureLogRequest.getFailureCode(), "FailureCode cannot be null");
		Assert.notNull(concurrentProcessFailureLogRequest.getFailureReason(), "FailureReason cannot be null");
		Assert.notNull(concurrentProcessFailureLogRequest.getFailureDateTime(), "FailureDateTime cannot be null");
		Assert.notNull(concurrentProcessFailureLogRequest.getFailureRecordId(), "FailureRecordId cannot be null");
		Assert.notNull(concurrentProcessFailureLogRequest.getFailureStep(), "FailureStep cannot be null");
	}

}