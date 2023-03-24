package com.ds.migration.processstart.consumer.domain;

import java.math.BigInteger;
import java.util.List;

import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncRecordData {

	List<BigInteger> failedSignedDraftIds = null;
	List<MigrationAuditDataRequest> migrationAuditDataRequestList = null;
	List<MigrationRecordIdInformation> migrationRecordIdInformationList = null;
}