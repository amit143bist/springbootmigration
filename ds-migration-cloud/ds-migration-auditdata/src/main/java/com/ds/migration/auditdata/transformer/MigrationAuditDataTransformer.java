package com.ds.migration.auditdata.transformer;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import com.ds.migration.auditdata.model.MigrationAuditEntries;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MigrationAuditDataTransformer {

	@Autowired
	ObjectMapper objectMapper;

	public MigrationAuditEntries tranformToMigrationAuditEntries(MigrationAuditDataRequest migrationAuditDataRequest)
			throws JsonProcessingException {

		MigrationAuditEntries migrationAuditEntries = new MigrationAuditEntries();
		migrationAuditEntries.setRecordId(migrationAuditDataRequest.getRecordId());
		migrationAuditEntries.setProcessId(UUID.fromString(migrationAuditDataRequest.getProcessId()));
		migrationAuditEntries.setRecordPhaseStatus(migrationAuditDataRequest.getRecordPhaseStatus());
		migrationAuditEntries.setRecordPhase(migrationAuditDataRequest.getRecordPhase());
		migrationAuditEntries.setAuditEntryDateTime(
				MigrationDateTimeUtil.convertToLocalDateTime(migrationAuditDataRequest.getAuditEntryDateTime()));

		byte[] digest = DigestUtils
				.md5Digest(objectMapper.writeValueAsString(migrationAuditDataRequest).toUpperCase().getBytes());
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {

			sb.append(Integer.toHexString((int) (b & 0xff)));
		}

		migrationAuditEntries.setHashedEntry(sb.toString());

		log.debug("tranformToMigrationAuditEntries called for recordId - {} and processId {}",
				migrationAuditDataRequest.getRecordId(), migrationAuditDataRequest.getProcessId());

		return migrationAuditEntries;
	}

	public MigrationAuditDataResponse tranformToMigrationAuditDataResponse(
			MigrationAuditEntries migrationAuditEntries) {

		MigrationAuditDataResponse migrationAuditDataResponse = new MigrationAuditDataResponse();

		migrationAuditDataResponse.setAuditId(migrationAuditEntries.getAuditId().toString());
		migrationAuditDataResponse.setProcessId(migrationAuditEntries.getProcessId().toString());
		migrationAuditDataResponse.setRecordId(migrationAuditEntries.getRecordId());
		migrationAuditDataResponse.setRecordPhaseStatus(migrationAuditEntries.getRecordPhaseStatus());
		migrationAuditDataResponse.setRecordPhase(migrationAuditEntries.getRecordPhase());
		migrationAuditDataResponse.setAuditEntryDateTime(
				MigrationDateTimeUtil.convertToString(migrationAuditEntries.getAuditEntryDateTime()));

		return migrationAuditDataResponse;
	}
}