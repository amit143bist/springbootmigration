package com.ds.migration.feign.auditdata.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataResponse;
import com.ds.migration.feign.auditdata.domain.MigrationAuditValidationInformation;

public interface MigrationAuditDataService {

	@PostMapping("/migration/audit")
	ResponseEntity<MigrationAuditDataResponse> saveAuditData(
			@RequestBody MigrationAuditDataRequest migrationAuditDataRequest);

	@PostMapping("/migration/audit/saveall")
	ResponseEntity<String> saveAllAuditData(@RequestBody MigrationAuditDataDefinition migrationAuditDataDefinition);

	@PostMapping("/migration/audit/saveallprocedure")
	ResponseEntity<String> saveAllAuditDataProcedure(
			@RequestBody MigrationAuditDataDefinition migrationAuditDataDefinition);

	@PutMapping("/migration/audit/{auditId}/validate")
	ResponseEntity<String> validateAuditEntry(@PathVariable String auditId);

	@PutMapping("/migration/audit/record/{recordId}/validate")
	ResponseEntity<MigrationAuditValidationInformation> validateRecordEntry(@PathVariable String recordId);

}