package com.ds.migration.coredata.validator;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.validator.IMigrationValidator;

@Service
public class ProcessLogValidator implements IMigrationValidator<ConcurrentProcessLogDefinition> {

	@Override
	public void validateSaveData(ConcurrentProcessLogDefinition concurrentProcessLogRequest) {

		Assert.notNull(concurrentProcessLogRequest.getBatchId(), "BatchId cannot be null");
		Assert.notNull(concurrentProcessLogRequest.getProcessStatus(), "ProcessStatus cannot be null");
		Assert.notNull(concurrentProcessLogRequest.getTotalRecordsInProcess(), "TotalRecordsInProcess cannot be null");
	}

}