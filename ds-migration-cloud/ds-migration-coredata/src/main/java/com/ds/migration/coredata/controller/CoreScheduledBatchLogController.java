package com.ds.migration.coredata.controller;

import java.math.BigInteger;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.common.exception.ResourceNotSavedException;
import com.ds.migration.common.exception.RunningBatchException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.coredata.model.CoreScheduledBatchLog;
import com.ds.migration.coredata.repository.CoreScheduledBatchLogRepository;
import com.ds.migration.coredata.transformer.CoreScheduledBatchLogTransformer;
import com.ds.migration.coredata.validator.BatchLogValidator;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogsInformation;
import com.ds.migration.feign.coredata.service.CoreScheduledBatchLogService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class CoreScheduledBatchLogController implements CoreScheduledBatchLogService {

	@Autowired
	CoreScheduledBatchLogRepository batchLogRepository;

	@Autowired
	CoreScheduledBatchLogTransformer coreScheduledBatchLogTransformer;

	@Autowired
	BatchLogValidator batchLogValidator;

	@Override
	public ResponseEntity<ScheduledBatchLogResponse> saveBatch(ScheduledBatchLogRequest scheduledBatchLogRequest) {

		batchLogValidator.validateSaveData(scheduledBatchLogRequest);
		CoreScheduledBatchLog coreScheduledBatchLog = coreScheduledBatchLogTransformer
				.transformToCoreScheduledBatchLog(scheduledBatchLogRequest);

		log.debug("scheduledBatchLogRequest transformed to coreScheduledBatchLog for batchType -> {}",
				scheduledBatchLogRequest.getBatchType());
		return Optional.ofNullable(batchLogRepository.save(coreScheduledBatchLog)).map(savedCoreScheduledBatchLog -> {

			Assert.notNull(savedCoreScheduledBatchLog.getBatchId(),
					"BatchId cannot be null for batchType " + scheduledBatchLogRequest.getBatchType());

			return new ResponseEntity<ScheduledBatchLogResponse>(
					coreScheduledBatchLogTransformer.transformToScheduledBatchLogResponse(savedCoreScheduledBatchLog),
					HttpStatus.CREATED);
		}).orElseThrow(
				() -> new ResourceNotSavedException("Batch not saved for " + scheduledBatchLogRequest.getBatchType()));
	}

	@Override
	public ResponseEntity<ScheduledBatchLogResponse> updateBatch(String batchId) {

		log.debug("updateBatch called for batchId -> {}", batchId);

		return batchLogRepository.findById(UUID.fromString(batchId)).map(scheduledBatch -> {

			if (null != scheduledBatch.getBatchEndDateTime()) {

				log.warn("BatchEndDateTime should be null for batchId#-> {}", batchId);

				return new ResponseEntity<ScheduledBatchLogResponse>(coreScheduledBatchLogTransformer
						.transformToScheduledBatchLogResponse(batchLogRepository.save(scheduledBatch)),
						HttpStatus.ALREADY_REPORTED);

			}

			scheduledBatch.setBatchEndDateTime(LocalDateTime.now());

			return new ResponseEntity<ScheduledBatchLogResponse>(coreScheduledBatchLogTransformer
					.transformToScheduledBatchLogResponse(batchLogRepository.save(scheduledBatch)), HttpStatus.OK);
		}).orElseThrow(() -> new ResourceNotFoundException("No Batch found with batchId# " + batchId));
	}

	@Override
	public ResponseEntity<ScheduledBatchLogResponse> findInCompleteBatch(String batchType) {

		log.debug("findInCompleteBatch called for batchType -> {}", batchType);

		Iterable<CoreScheduledBatchLog> scheduledBatchLogList = batchLogRepository
				.findAllByBatchTypeAndBatchEndDateTimeIsNull(batchType);

		return Optional.ofNullable(scheduledBatchLogList).map(batchLogList -> {

			if (IterableUtil.isNullOrEmpty(batchLogList)) {

				throw new ResourceNotFoundException("No Batch running with batch type " + batchType);
			}

			if (IterableUtil.sizeOf(batchLogList) > 1) {

				throw new RunningBatchException("More than one batch is already running for batchType " + batchType);
			}

			CoreScheduledBatchLog batchLog = batchLogList.iterator().next();

			Assert.notNull(batchLog.getBatchId(), "BatchId cannot be null for batchType " + batchType);
			Assert.isNull(batchLog.getBatchEndDateTime(), "BatchEndDateTime should be null for batchType " + batchType);

			return new ResponseEntity<ScheduledBatchLogResponse>(
					coreScheduledBatchLogTransformer.transformToScheduledBatchLogResponse(batchLog), HttpStatus.OK);

		}).orElseThrow(() -> new ResourceNotFoundException("No Batch running with batch type " + batchType));
	}

	@Override
	public ResponseEntity<ScheduledBatchLogResponse> findBatchByBatchId(String batchId) {

		log.debug("findBatchByBatchId called for batchId -> {}", batchId);

		return new ResponseEntity<ScheduledBatchLogResponse>(
				coreScheduledBatchLogTransformer.transformToScheduledBatchLogResponse(
						batchLogRepository.findById(UUID.fromString(batchId)).map(scheduledBatch -> {

							return scheduledBatch;
						}).orElseThrow(
								() -> new ResourceNotFoundException("No Batch available for batchId# " + batchId))),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogResponse> findLatestBatchByBatchType(String batchType) {

		log.debug("findLatestBatchByBatchType called for batchType -> {}", batchType);

		return new ResponseEntity<ScheduledBatchLogResponse>(
				coreScheduledBatchLogTransformer.transformToScheduledBatchLogResponse(batchLogRepository
						.findTopByBatchTypeOrderByBatchStartDateTimeDesc(batchType).map(scheduledBatch -> {

							return scheduledBatch;
						}).orElseThrow(
								() -> new ResourceNotFoundException("No Batch running with batch type " + batchType))),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogsInformation> findAllInCompleteBatches(String batchType) {

		return prepareResponse(batchLogRepository.findAllByBatchTypeAndBatchEndDateTimeIsNull(batchType), batchType);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogsInformation> findAllBatchesByBatchType(String batchType) {

		return prepareResponse(batchLogRepository.findAllByBatchType(batchType), batchType);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogsInformation> findAllByBatchTypeAndBatchStartDateTimeBetween(
			String batchType, String fromDate, String toDate) {

		return prepareResponse(batchLogRepository.findAllByBatchTypeAndBatchStartDateTimeBetween(batchType,
				MigrationDateTimeUtil.convertToLocalDateTime(fromDate),
				MigrationDateTimeUtil.convertToLocalDateTime(toDate)), batchType);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogsInformation> findAllBatches() {

		return prepareResponse(batchLogRepository.findAll(), null);
	}

	@Override
	public ResponseEntity<ScheduledBatchLogsInformation> findAllCompleteBatches(String batchType) {

		return prepareResponse(batchLogRepository.findAllByBatchTypeAndBatchEndDateTimeIsNotNull(batchType), batchType);
	}

	private ResponseEntity<ScheduledBatchLogsInformation> prepareResponse(
			Iterable<CoreScheduledBatchLog> coreScheduledBatchLogIterable, String batchType) {

		log.debug("prepareResponse is called for batchType -> {}", batchType);
		if (IterableUtil.isNullOrEmpty(coreScheduledBatchLogIterable)) {

			if (null != batchType) {
				throw new ResourceNotFoundException("No Batch exists for batchType " + batchType);
			} else {
				throw new ResourceNotFoundException("No Batch exists");
			}
		}

		List<ScheduledBatchLogResponse> scheduledBatchLogResponseList = new ArrayList<ScheduledBatchLogResponse>();
		coreScheduledBatchLogIterable.forEach(coreScheduledBatchLog -> {

			scheduledBatchLogResponseList
					.add(coreScheduledBatchLogTransformer.transformToScheduledBatchLogResponse(coreScheduledBatchLog));
		});

		ScheduledBatchLogsInformation scheduledBatchLogsInformation = new ScheduledBatchLogsInformation();
		scheduledBatchLogsInformation.setScheduledBatchLogResponses(scheduledBatchLogResponseList);

		if (null != scheduledBatchLogResponseList && !scheduledBatchLogResponseList.isEmpty()) {

			scheduledBatchLogsInformation
					.setTotalBatchesCount(BigInteger.valueOf(scheduledBatchLogResponseList.size()));
		}

		return new ResponseEntity<ScheduledBatchLogsInformation>(scheduledBatchLogsInformation, HttpStatus.OK);
	}

}