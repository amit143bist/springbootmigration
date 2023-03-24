package com.ds.migration.feign.prontodata.service;

import java.math.BigInteger;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataRequest;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataResponse;
import com.ds.migration.feign.prontodata.domain.MigrationSignedDraftDefinition;
import com.ds.migration.feign.prontodata.domain.ProntoDataInformation;

public interface MigrationProntoDataService {

	@GetMapping(value = "/migration/prontodata/drafts")
	ResponseEntity<BigInteger[]> signedDraftList(
			@RequestParam(name = "beginDateTime", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beginDateTime,
			@RequestParam(name = "endDateTime", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime);

	@GetMapping("/migration/prontodata/signedraft/{id}")
	ResponseEntity<MigrationProntoDataResponse[]> signedDraft(@PathVariable("id") Long id);

	@PutMapping("/migration/prontodata/allsignedraftdetails")
	ResponseEntity<ProntoDataInformation> fetchAllSignedDraftDetails(
			@RequestBody MigrationSignedDraftDefinition migrationSignedDraftDefinition);

	@PutMapping("/migration/prontodata/signedraft/{id}")
	ResponseEntity<String> updateSignedDraftWithDocuSignId(@RequestBody MigrationProntoDataRequest request,
			@PathVariable("id") Long id);

	@PutMapping("/migration/prontodata/update/allsignedrafts")
	ResponseEntity<MigrationRecordIdInformationDefinition> updateAllSignedDraftWithDocuSignId(
			@RequestBody MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition);

}