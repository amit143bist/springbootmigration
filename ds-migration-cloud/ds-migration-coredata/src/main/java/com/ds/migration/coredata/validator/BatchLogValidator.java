package com.ds.migration.coredata.validator;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.validator.IMigrationValidator;

@Service
public class BatchLogValidator implements IMigrationValidator<ScheduledBatchLogRequest> {

	@Override
	public void validateSaveData(ScheduledBatchLogRequest migrationInformation) {

		Assert.notNull(migrationInformation.getBatchType(), "BatchType cannot be null");
		Assert.notNull(migrationInformation.getBatchStartParameters(), "BatchStartParameters cannot be null");
	}

}