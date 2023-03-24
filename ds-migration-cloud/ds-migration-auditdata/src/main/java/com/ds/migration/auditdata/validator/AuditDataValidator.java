package com.ds.migration.auditdata.validator;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.validator.IMigrationValidator;

@Service
public class AuditDataValidator implements IMigrationValidator<MigrationAuditDataRequest> {

	@Override
	public void validateSaveData(MigrationAuditDataRequest migrationAuditDataRequest) {

		Assert.notNull(migrationAuditDataRequest.getRecordId(), "RecordId cannot be null");
		Assert.notNull(migrationAuditDataRequest.getProcessId(), "ProcessId cannot be null");
		Assert.notNull(migrationAuditDataRequest.getAuditEntryDateTime(), "AuditEntryDateTime cannot be null");
		Assert.notNull(migrationAuditDataRequest.getRecordPhaseStatus(), "RecordPhaseStatus cannot be null");
		Assert.notNull(migrationAuditDataRequest.getRecordPhase(), "RecordPhase cannot be null");
	}

}