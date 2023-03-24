package com.ds.migration.auditdata.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.assertj.core.util.IterableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.auditdata.model.MigrationAuditEntries;
import com.ds.migration.auditdata.repository.MigrationAuditDataRepository;
import com.ds.migration.auditdata.transformer.MigrationAuditDataTransformer;
import com.ds.migration.auditdata.validator.AuditDataValidator;
import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.common.exception.ResourceConditionFailedException;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.common.exception.ResourceNotSavedException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataResponse;
import com.ds.migration.feign.auditdata.domain.MigrationAuditValidationInformation;
import com.ds.migration.feign.auditdata.service.MigrationAuditDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class MigrationAuditDataController implements MigrationAuditDataService {

	@Autowired
	MigrationAuditDataRepository migrationAuditDataRepository;

	@Autowired
	MigrationAuditDataTransformer migrationAuditDataTransformer;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	AuditDataValidator auditDataValidator;

	@Override
	public ResponseEntity<MigrationAuditDataResponse> saveAuditData(
			MigrationAuditDataRequest migrationAuditDataRequest) {

		try {

			log.info("Calling saveAuditData with recordId {}", migrationAuditDataRequest.getRecordId());

			auditDataValidator.validateSaveData(migrationAuditDataRequest);
			MigrationAuditEntries migrationAuditEntries = migrationAuditDataTransformer
					.tranformToMigrationAuditEntries(migrationAuditDataRequest);

			return Optional.ofNullable(migrationAuditDataRepository.save(migrationAuditEntries))
					.map(savedMigrationAuditEntries -> {

						Assert.notNull(savedMigrationAuditEntries.getAuditId(),
								"AuditId cannot be null for recordId# " + migrationAuditDataRequest.getRecordId());

						log.info("Completed saveAuditData with recordId {}", migrationAuditDataRequest.getRecordId());

						return new ResponseEntity<MigrationAuditDataResponse>(migrationAuditDataTransformer
								.tranformToMigrationAuditDataResponse(savedMigrationAuditEntries), HttpStatus.CREATED);
					}).orElseThrow(() -> new ResourceNotSavedException(
							"Audit entry not saved for recordId# " + migrationAuditDataRequest.getRecordId()));
		} catch (JsonProcessingException e) {

			log.error("JsonProcessingException thrown when saving Audit entry for recordId# {}",
					migrationAuditDataRequest.getRecordId());
			throw new ResourceConditionFailedException("JsonProcessingException " + e.getMessage()
					+ " occurred when saving Audit entry for recordId# " + migrationAuditDataRequest.getRecordId());
		}

	}

	@Override
	public ResponseEntity<String> saveAllAuditData(MigrationAuditDataDefinition migrationAuditDataDefinition) {

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = migrationAuditDataDefinition
				.getMigrationAuditDataRequestList();

		List<MigrationAuditEntries> migrationAuditEntriesList = new ArrayList<MigrationAuditEntries>(
				migrationAuditDataRequestList.size());

		log.info(
				"Received JSON processing in saveAllAuditData() for processId -> {}, messagesize is {} and firstRecordId is {}",
				migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
				migrationAuditDataRequestList.get(0).getRecordId());

		for (MigrationAuditDataRequest migrationAuditDataRequest : migrationAuditDataRequestList) {

			try {

				auditDataValidator.validateSaveData(migrationAuditDataRequest);
				migrationAuditEntriesList
						.add(migrationAuditDataTransformer.tranformToMigrationAuditEntries(migrationAuditDataRequest));

			} catch (JsonProcessingException e) {

				log.error("JsonProcessingException thrown when saving Audit entry for recordId# {}",
						migrationAuditDataRequest.getRecordId());
				throw new ResourceConditionFailedException("JsonProcessingException " + e.getMessage()
						+ " occurred when saving Audit entry for recordId# " + migrationAuditDataRequest.getRecordId());
			}
		}

		return Optional.ofNullable(migrationAuditDataRepository.saveAll(migrationAuditEntriesList))
				.map(savedMigrationAuditEntries -> {

					log.info(
							"Completed data processing in saveAllAuditData() for processId -> {}, messagesize is {} and firstRecordId is {}",
							migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
							migrationAuditDataRequestList.get(0).getRecordId());

					return new ResponseEntity<String>(MigrationAppConstants.SUCCESS_VALUE, HttpStatus.CREATED);
				}).orElseThrow(() -> new ResourceNotSavedException(

						"Audit entry not saved for processId# " + migrationAuditDataDefinition.getProcessId()
								+ "and firstRecordId is " + migrationAuditDataRequestList.get(0).getRecordId()));

	}

	@Override
	public ResponseEntity<String> saveAllAuditDataProcedure(MigrationAuditDataDefinition migrationAuditDataDefinition) {

		List<MigrationAuditDataRequest> migrationAuditDataRequestList = migrationAuditDataDefinition
				.getMigrationAuditDataRequestList();

		log.info(
				"Received JSON processing in saveAllAuditDataProcedure() for processId -> {}, messagesize is {} and firstRecordId is {}",
				migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
				migrationAuditDataRequestList.get(0).getRecordId());

		for (MigrationAuditDataRequest migrationAuditDataRequest : migrationAuditDataRequestList) {

			try {

				auditDataValidator.validateSaveData(migrationAuditDataRequest);

				byte[] digest = DigestUtils
						.md5Digest(objectMapper.writeValueAsString(migrationAuditDataRequest).toUpperCase().getBytes());
				StringBuilder sb = new StringBuilder();
				for (byte b : digest) {

					sb.append(Integer.toHexString((int) (b & 0xff)));
				}

				migrationAuditDataRepository.createMigrationAuditData(UUID.randomUUID(),
						migrationAuditDataRequest.getRecordId(),
						UUID.fromString(migrationAuditDataRequest.getProcessId()),
						MigrationDateTimeUtil.convertToLocalDateTime(migrationAuditDataRequest.getAuditEntryDateTime()),
						migrationAuditDataRequest.getRecordPhaseStatus(), migrationAuditDataRequest.getRecordPhase(),
						sb.toString());

			} catch (JsonProcessingException e) {

				log.error("JsonProcessingException thrown when saving Audit entry for recordId# {}",
						migrationAuditDataRequest.getRecordId());
				throw new ResourceConditionFailedException("JsonProcessingException " + e.getMessage()
						+ " occurred when saving Audit entry for recordId# " + migrationAuditDataRequest.getRecordId()
						+ "and firstRecordId is " + migrationAuditDataRequestList.get(0).getRecordId());
			}
		}

		log.info(
				"Completed data processing in saveAllAuditDataProcedure() for processId -> {}, messagesize is {} and firstRecordId is {}",
				migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
				migrationAuditDataRequestList.get(0).getRecordId());

		return new ResponseEntity<String>(MigrationAppConstants.SUCCESS_VALUE, HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<String> validateAuditEntry(String auditId) {

		log.debug("Validating AuditEntry for {}", auditId);

		return migrationAuditDataRepository.findById(UUID.fromString(auditId)).map(auditEntry -> {

			MigrationAuditDataResponse migrationAuditDataResponse = migrationAuditDataTransformer
					.tranformToMigrationAuditDataResponse(auditEntry);

			migrationAuditDataResponse.setAuditId(null);

			try {

				log.debug("Reading hashedEntry for {}", objectMapper.writeValueAsString(migrationAuditDataResponse));

				byte[] digest = DigestUtils.md5Digest(
						objectMapper.writeValueAsString(migrationAuditDataResponse).toUpperCase().getBytes());
				StringBuilder sb = new StringBuilder();
				for (byte b : digest) {

					sb.append(Integer.toHexString((int) (b & 0xff)));
				}

				if ((sb.toString()).equals(auditEntry.getHashedEntry())) {
					return new ResponseEntity<String>(ValidationResult.SUCCESS.toString(), HttpStatus.OK);
				} else {
					return new ResponseEntity<String>(ValidationResult.FAILED.toString(),
							HttpStatus.UNPROCESSABLE_ENTITY);
				}

			} catch (JsonProcessingException e) {

				log.error("JsonProcessingException occurred when validating Audit entry for {}", auditId);
				throw new ResourceConditionFailedException("JsonProcessingException " + e.getMessage()
						+ " occurred when validating Audit entry for " + auditId);
			}

		}).orElseThrow(() -> new ResourceNotFoundException("Audit entry not found for auditId# " + auditId));
	}

	@Override
	public ResponseEntity<MigrationAuditValidationInformation> validateRecordEntry(String recordId) {

		log.debug("Validating AuditEntries for {}", recordId);

		return Optional.ofNullable(migrationAuditDataRepository.findAllByRecordId(recordId)).map(dbAuditEntries -> {

			if (IterableUtil.isNullOrEmpty(dbAuditEntries)) {

				log.error("dbAuditEntries is null or empty for inputRecord# {}", recordId);
				throw new ResourceNotFoundException("dbAuditEntries is empty or null for inputRecord# " + recordId);
			}

			List<String> auditIds = new ArrayList<String>();
			dbAuditEntries.forEach(auditEntry -> {

				MigrationAuditDataResponse migrationAuditDataResponse = migrationAuditDataTransformer
						.tranformToMigrationAuditDataResponse(auditEntry);

				String auditId = auditEntry.getAuditId().toString();
				migrationAuditDataResponse.setAuditId(null);

				try {

					log.debug("Reading hashedEntry for {}",
							objectMapper.writeValueAsString(migrationAuditDataResponse));

					byte[] digest = DigestUtils.md5Digest(
							objectMapper.writeValueAsString(migrationAuditDataResponse).toUpperCase().getBytes());
					StringBuilder sb = new StringBuilder();
					for (byte b : digest) {

						sb.append(Integer.toHexString((int) (b & 0xff)));
					}

					log.info("auditId " + auditId);
					log.info("sb.toString() " + sb.toString());
					log.info("auditEntry.getHashedEntry()" + auditEntry.getHashedEntry());

					if (!(sb.toString()).equals(auditEntry.getHashedEntry())) {

						auditIds.add(auditId);
					}
				} catch (JsonProcessingException e) {

					log.error("JsonProcessingException occurred when validating Audit entry for {} for inputRecord {}",
							auditId, recordId);
					throw new ResourceConditionFailedException("JsonProcessingException " + e.getMessage()
							+ " occurred when validating Audit entry for " + auditId + " for inputRecord " + recordId);
				}
			});

			MigrationAuditValidationInformation migrationAuditValidationInformation = new MigrationAuditValidationInformation();

			if (auditIds.isEmpty()) {

				migrationAuditValidationInformation.setStatus(ValidationResult.SUCCESS.toString());
				return new ResponseEntity<MigrationAuditValidationInformation>(migrationAuditValidationInformation,
						HttpStatus.OK);
			} else {

				migrationAuditValidationInformation.setAuditIds(auditIds);
				migrationAuditValidationInformation.setStatus(ValidationResult.FAILED.toString());

				return new ResponseEntity<MigrationAuditValidationInformation>(migrationAuditValidationInformation,
						HttpStatus.PRECONDITION_FAILED);
			}
		}).orElseThrow(() -> new ResourceNotFoundException("Audit entries not found for recordId# " + recordId));

	}

}