package com.ds.migration.feign.auditdata.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;

public interface MigrationRecordIdService {

	@PostMapping("/migration/records")
	ResponseEntity<String> saveRecordIdData(@RequestBody MigrationRecordIdInformation migrationRecordIdInformation);

	@PostMapping("/migration/records/saveall")
	ResponseEntity<String> saveAllRecordIdData(
			@RequestBody MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition);

	@GetMapping("/migration/records/{recordId}")
	ResponseEntity<MigrationRecordIdInformation> findRecordId(@PathVariable String recordId);
}