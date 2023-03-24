package com.ds.migration.coredata.controller;

import java.math.BigInteger;
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

import com.ds.migration.common.constant.RetryStatus;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.common.exception.ResourceNotSavedException;
import com.ds.migration.coredata.model.CoreProcessFailureLog;
import com.ds.migration.coredata.repository.CoreProcessFailureLogRepository;
import com.ds.migration.coredata.transformer.CoreProcessFailureLogTransformer;
import com.ds.migration.coredata.validator.FailureLogValidator;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogsInformation;
import com.ds.migration.feign.coredata.service.CoreProcessFailureLogService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class CoreProcessFailureLogController implements CoreProcessFailureLogService {

	@Autowired
	CoreProcessFailureLogRepository coreProcessFailureLogRepository;

	@Autowired
	CoreProcessFailureLogTransformer coreProcessFailureLogTransformer;

	@Autowired
	FailureLogValidator failureLogValidator;

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogDefinition> saveFailureLog(
			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogRequest) {

		failureLogValidator.validateSaveData(concurrentProcessFailureLogRequest);
		CoreProcessFailureLog coreProcessFailureLog = coreProcessFailureLogTransformer
				.transformToCoreProcessFailureLog(concurrentProcessFailureLogRequest);

		log.debug("concurrentProcessFailureLogRequest transformed to coreProcessFailureLog for failureRecordId -> {}",
				concurrentProcessFailureLogRequest.getFailureRecordId());
		return Optional.ofNullable(coreProcessFailureLogRepository.save(coreProcessFailureLog)).map(failureLog -> {

			Assert.notNull(failureLog.getProcessFailureId(), "ProcessFailureId cannot be null for failureRecordId# "
					+ concurrentProcessFailureLogRequest.getFailureRecordId());

			return new ResponseEntity<ConcurrentProcessFailureLogDefinition>(
					coreProcessFailureLogTransformer.transformToConcurrentProcessFailureLogResponse(failureLog),
					HttpStatus.CREATED);
		}).orElseThrow(() -> new ResourceNotSavedException(
				"Failure Log not saved for " + concurrentProcessFailureLogRequest.getFailureRecordId()));
	}

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogDefinition> updateFailureLog(
			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogRequest, String processFailureId) {

		log.debug("updateFailureLog called for processFailureId -> {}", processFailureId);

		return coreProcessFailureLogRepository.findById(UUID.fromString(processFailureId)).map(failureLog -> {

			coreProcessFailureLogTransformer.updateFailureLogData(concurrentProcessFailureLogRequest, failureLog);

			CoreProcessFailureLog savedCoreProcessFailureLog = coreProcessFailureLogRepository.save(failureLog);
			return new ResponseEntity<ConcurrentProcessFailureLogDefinition>(coreProcessFailureLogTransformer
					.transformToConcurrentProcessFailureLogResponse(savedCoreProcessFailureLog), HttpStatus.OK);
		}).orElseThrow(() -> new ResourceNotFoundException(
				"No ProcessFailure found with processFailureId# " + processFailureId));
	}

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLog() {

		log.debug("listAllProcessFailureLog called");

		Iterable<CoreProcessFailureLog> coreProcessFailureLogList = coreProcessFailureLogRepository
				.findAllByRetryStatusOrRetryStatusIsNull(RetryStatus.F.toString());

		if (IterableUtil.isNullOrEmpty(coreProcessFailureLogList)) {

			throw new ResourceNotFoundException("CoreProcessFailureLogList is empty or null");
		}

		return prepareResponse(coreProcessFailureLogList);
	}

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLogForConcurrentProcessId(
			String processId) {

		log.debug("listAllProcessFailureLogForConcurrentProcessId called for processId -> {}", processId);

		Iterable<CoreProcessFailureLog> coreProcessFailureLogList = coreProcessFailureLogRepository
				.findAllByProcessIdAndRetryStatusOrProcessIdAndRetryStatusIsNull(UUID.fromString(processId),
						RetryStatus.F.toString(), UUID.fromString(processId));

		if (IterableUtil.isNullOrEmpty(coreProcessFailureLogList)) {

			throw new ResourceNotFoundException(
					"CoreProcessFailureLogList is empty or null for processId# " + processId);
		}

		return prepareResponse(coreProcessFailureLogList);
	}

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLogForFailureRecordId(
			String failureRecordId) {

		log.debug("listAllProcessFailureLogForFailureRecordId called for failureRecordId -> {}", failureRecordId);

		Iterable<CoreProcessFailureLog> coreProcessFailureLogList = coreProcessFailureLogRepository
				.findAllByFailureRecordIdAndRetryStatusOrFailureRecordIdAndRetryStatusIsNull(failureRecordId,
						RetryStatus.F.toString(), failureRecordId);

		if (IterableUtil.isNullOrEmpty(coreProcessFailureLogList)) {

			throw new ResourceNotFoundException(
					"CoreProcessFailureLogList is empty or null for failurerecordId# " + failureRecordId);
		}

		return prepareResponse(coreProcessFailureLogList);
	}

	@Override
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailuresByProcessIds(
			List<UUID> processIds) {

		return prepareResponse(
				coreProcessFailureLogRepository.findAllByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(
						processIds, RetryStatus.F.toString(), processIds));
	}

	@Override
	public ResponseEntity<Long> countProcessFailuresByProcessIds(List<UUID> processIds) {

		log.debug("countProcessFailuresByProcessIds called");

		return Optional.ofNullable(
				coreProcessFailureLogRepository.countByProcessIdInAndRetryStatusOrProcessIdInAndRetryStatusIsNull(
						processIds, RetryStatus.F.toString(), processIds))
				.map(failureCount -> {

					Assert.state(failureCount > -1, "ProcessCount should be greater than -1, check the processIds ");

					return new ResponseEntity<Long>(failureCount, HttpStatus.OK);
				}).orElseThrow(() -> new ResourceNotFoundException("No result returned"));
	}

	@Override
	public ResponseEntity<Long> countProcessFailures() {

		log.debug("countProcessFailures called");

		return Optional
				.ofNullable(
						coreProcessFailureLogRepository.countByRetryStatusOrRetryStatusIsNull(RetryStatus.F.toString()))
				.map(failureCount -> {

					Assert.state(failureCount > -1, "ProcessCount should be greater than -1 ");

					return new ResponseEntity<Long>(failureCount, HttpStatus.OK);
				}).orElseThrow(() -> new ResourceNotFoundException("No result returned"));
	}

	private ResponseEntity<ConcurrentProcessFailureLogsInformation> prepareResponse(
			Iterable<CoreProcessFailureLog> coreProcessFailureLogList) {

		log.debug("prepareResponse called");

		List<ConcurrentProcessFailureLogDefinition> concurrentProcessFailureLogResponseList = new ArrayList<ConcurrentProcessFailureLogDefinition>();

		coreProcessFailureLogList.forEach(failureLog -> {

			concurrentProcessFailureLogResponseList
					.add(coreProcessFailureLogTransformer.transformToConcurrentProcessFailureLogResponse(failureLog));
		});

		ConcurrentProcessFailureLogsInformation concurrentProcessFailureLogsInformation = new ConcurrentProcessFailureLogsInformation();
		concurrentProcessFailureLogsInformation
				.setConcurrentProcessFailureLogDefinitions(concurrentProcessFailureLogResponseList);

		if (null != concurrentProcessFailureLogResponseList && !concurrentProcessFailureLogResponseList.isEmpty()) {

			concurrentProcessFailureLogsInformation
					.setTotalFailureCount(BigInteger.valueOf(concurrentProcessFailureLogResponseList.size()));
		}

		return new ResponseEntity<ConcurrentProcessFailureLogsInformation>(concurrentProcessFailureLogsInformation,
				HttpStatus.OK);
	}

}