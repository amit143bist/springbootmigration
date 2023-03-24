package com.ds.migration.auditdata.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.auditdata.model.MigrationRecordIdEntries;
import com.ds.migration.auditdata.repository.MigrationRecordIdEntriesRepository;
import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.common.exception.ResourceNotSavedException;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.auditdata.service.MigrationRecordIdService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class MigrationRecordIdController implements MigrationRecordIdService {

	@Autowired
	MigrationRecordIdEntriesRepository migrationRecordIdEntriesRepository;

	@Override
	public ResponseEntity<String> saveRecordIdData(MigrationRecordIdInformation migrationRecordIdInformation) {

		log.info("Received JSON processing in saveRecordIdData() for docuSignId -> {} and recordId is {}",
				migrationRecordIdInformation.getDocuSignId(), migrationRecordIdInformation.getRecordId());

		MigrationRecordIdEntries recordIdEntries = new MigrationRecordIdEntries(
				migrationRecordIdInformation.getRecordId(),
				UUID.fromString(migrationRecordIdInformation.getDocuSignId()));

		return Optional.ofNullable(migrationRecordIdEntriesRepository.save(recordIdEntries)).map(savedEntry -> {

			log.info("Completed data processing in saveRecordIdData() for docuSignId -> {} and recordId is {}",
					migrationRecordIdInformation.getDocuSignId(), migrationRecordIdInformation.getRecordId());

			return new ResponseEntity<String>(savedEntry.getRecordId(), HttpStatus.CREATED);

		}).orElseThrow(() -> new ResourceNotSavedException(
				"MigrationRecordId entry not saved for " + migrationRecordIdInformation.getRecordId()));
	}

	@Override
	public ResponseEntity<String> saveAllRecordIdData(
			MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition) {

		List<MigrationRecordIdInformation> migrationRecordIdInformationList = migrationRecordIdInformationDefinition
				.getMigrationRecordIdInformationList();

		log.info(
				"Received JSON processing in saveAllRecordIdData() for processId -> {}, messagesize is {} and firstRecordId is {}",
				migrationRecordIdInformationDefinition.getProcessId(),
				migrationRecordIdInformationDefinition.getTotalRecords(),
				migrationRecordIdInformationList.get(0).getRecordId());

		List<MigrationRecordIdEntries> migrationRecordIdEntriesList = new ArrayList<MigrationRecordIdEntries>();
		for (MigrationRecordIdInformation migrationRecordIdInformation : migrationRecordIdInformationList) {

			migrationRecordIdEntriesList.add(new MigrationRecordIdEntries(migrationRecordIdInformation.getRecordId(),
					UUID.fromString(migrationRecordIdInformation.getDocuSignId())));
		}

		return Optional.ofNullable(migrationRecordIdEntriesRepository.saveAll(migrationRecordIdEntriesList))
				.map(savedEntry -> {

					log.info(
							"Completed data processing in saveAllRecordIdData() for processId -> {}, messagesize is {} and firstRecordId is {}",
							migrationRecordIdInformationDefinition.getProcessId(),
							migrationRecordIdInformationDefinition.getTotalRecords(),
							migrationRecordIdInformationList.get(0).getRecordId());

					return new ResponseEntity<String>(MigrationAppConstants.SUCCESS_VALUE, HttpStatus.CREATED);

				})
				.orElseThrow(() -> new ResourceNotSavedException("MigrationRecordId entries not saved for "
						+ migrationRecordIdInformationDefinition.getProcessId() + "and firstRecordId is "
						+ migrationRecordIdInformationList.get(0).getRecordId()));
	}

	@Override
	public ResponseEntity<MigrationRecordIdInformation> findRecordId(String recordId) {

		return migrationRecordIdEntriesRepository.findById(recordId).map(recordIdEntry -> {

			return new ResponseEntity<MigrationRecordIdInformation>(new MigrationRecordIdInformation(
					recordIdEntry.getRecordId(), recordIdEntry.getDocusignId().toString()), HttpStatus.OK);
		}).orElseThrow(
				() -> new ResourceNotFoundException("No Record found in MigrationRecordIdEntries for " + recordId));

	}

}