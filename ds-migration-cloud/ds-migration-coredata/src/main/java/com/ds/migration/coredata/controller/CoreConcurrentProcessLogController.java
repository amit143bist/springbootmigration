package com.ds.migration.coredata.controller;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;

import org.assertj.core.util.IterableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.common.exception.ResourceNotSavedException;
import com.ds.migration.coredata.model.CoreConcurrentProcessLog;
import com.ds.migration.coredata.repository.CoreConcurrentProcessLogRepository;
import com.ds.migration.coredata.transformer.ConcurrentProcessLogTransformer;
import com.ds.migration.coredata.validator.ProcessLogValidator;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;
import com.ds.migration.feign.coredata.service.CoreConcurrentProcessLogService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class CoreConcurrentProcessLogController implements CoreConcurrentProcessLogService {

	@Autowired
	CoreConcurrentProcessLogRepository concurrentProcessLogRepository;

	@Autowired
	ConcurrentProcessLogTransformer concurrentProcessLogTransformer;

	@Autowired
	ProcessLogValidator processLogValidator;

	@Autowired
	private HazelcastInstance hazelcast;

	@Value("${migration.application.hazelcast.lockduration}")
	private long lockDuration;// in milliseconds

	@Override
	public ResponseEntity<ConcurrentProcessLogDefinition> saveConcurrentProcess(
			ConcurrentProcessLogDefinition concurrentProcessLogRequest) {

		processLogValidator.validateSaveData(concurrentProcessLogRequest);
		CoreConcurrentProcessLog coreConcurrentProcessLog = concurrentProcessLogTransformer
				.tranformToCoreConcurrentProcessLog(concurrentProcessLogRequest);

		log.debug("concurrentProcessLogRequest transformed to coreConcurrentProcessLog for batchId -> {}",
				concurrentProcessLogRequest.getBatchId());
		return Optional.ofNullable(concurrentProcessLogRepository.save(coreConcurrentProcessLog))
				.map(savedCoreConcurrentProcessLog -> {

					Assert.notNull(savedCoreConcurrentProcessLog.getProcessId(),
							"ProcessId cannot be null for batchId# " + concurrentProcessLogRequest.getBatchId());

					log.debug(
							"savedCoreConcurrentProcessLog successfully saved in CoreConcurrentProcessLogController.saveConcurrentProcess()");
					return new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogTransformer
							.tranformToConcurrentProcessLogResponse(savedCoreConcurrentProcessLog), HttpStatus.CREATED);
				})
				.orElseThrow(() -> new ResourceNotSavedException("Requested Concurrent Process not saved for batchId# "
						+ concurrentProcessLogRequest.getBatchId()));
	}

	@Override
	public ResponseEntity<ConcurrentProcessLogDefinition> updateConcurrentProcess(
			ConcurrentProcessLogDefinition concurrentProcessLogRequest, String processId) {

		log.debug("updateConcurrentProcess called for processId -> {}", processId);

		return concurrentProcessLogRepository.findById(UUID.fromString(processId)).map(coreConcurrentProcess -> {

			if (null != coreConcurrentProcess.getProcessEndDateTime()) {

				log.warn("ProcessEndDateTime should be null for processId-> {}", processId);
				return new ResponseEntity<ConcurrentProcessLogDefinition>(
						concurrentProcessLogTransformer.tranformToConcurrentProcessLogResponse(coreConcurrentProcess),
						HttpStatus.ALREADY_REPORTED);

			}

			coreConcurrentProcess.setProcessEndDateTime(LocalDateTime.now());
			coreConcurrentProcess.setProcessStatus(concurrentProcessLogRequest.getProcessStatus());

			CoreConcurrentProcessLog savedCoreConcurrentProcessLog = null;

			IMap<String, String> map = hazelcast.getMap("PROCESS_LOCKS");
			map.lock(MigrationAppConstants.SAVE_LOCK_NAME, lockDuration, TimeUnit.MILLISECONDS);

			try {

				savedCoreConcurrentProcessLog = concurrentProcessLogRepository.save(coreConcurrentProcess);
			} catch (Exception exp) {

				unlockKey(map, MigrationAppConstants.SAVE_LOCK_NAME);
				log.error(
						"Lock error occurred {}, message is {}, requested Concurrent Process not saved for batchId# {}",
						exp.getCause(), exp.getMessage(), concurrentProcessLogRequest.getBatchId());
				throw new ResourceNotSavedException(
						"Lock error occurred, requested Concurrent Process not saved for batchId# "
								+ concurrentProcessLogRequest.getBatchId());
			} finally {

				unlockKey(map, MigrationAppConstants.SAVE_LOCK_NAME);
			}

			return new ResponseEntity<ConcurrentProcessLogDefinition>(concurrentProcessLogTransformer
					.tranformToConcurrentProcessLogResponse(savedCoreConcurrentProcessLog), HttpStatus.OK);

		}).orElseThrow(() -> new ResourceNotFoundException("No Process found with processId# " + processId));
	}

	private void unlockKey(IMap<String, String> map, String key) {
		try {

			map.unlock("SAVE_LOCK");
		} catch (Exception exp) {

			log.error("Exception -> {} occurred in unlockKey for key -> {}", exp.getCause() + "_" + exp.getMessage(),
					key);
		}
	}

	@Override
	public ResponseEntity<Long> countPendingConcurrentProcessInBatch(String batchId) {

		log.debug("countPendingConcurrentProcessInBatch called for batchId -> {}", batchId);

		return Optional.ofNullable(
				concurrentProcessLogRepository.countByBatchIdAndProcessEndDateTimeIsNull(UUID.fromString(batchId)))
				.map(processCount -> {

					Assert.state(processCount > -1,
							"ProcessCount should be greater than -1, check the batchId# " + batchId);

					return new ResponseEntity<Long>(processCount, HttpStatus.OK);
				}).orElseThrow(() -> new ResourceNotFoundException("No result returned for batchId# " + batchId));
	}

	@Override
	public ResponseEntity<ConcurrentProcessLogsInformation> findAllProcessesForBatchId(String batchId) {

		log.debug("findAllBatchesForBatchId called for batchId -> {}", batchId);

		return prepareResponse(concurrentProcessLogRepository.findAllByBatchId(UUID.fromString(batchId)), batchId);

	}

	@Override
	public ResponseEntity<ConcurrentProcessLogsInformation> findAllInCompleteProcessesForBatchId(String batchId) {

		log.debug("findAllIncompleteBatchesForBatchId called for batchId -> {}", batchId);

		return prepareResponse(
				concurrentProcessLogRepository.findAllByBatchIdAndProcessEndDateTimeIsNull(UUID.fromString(batchId)),
				batchId);
	}

	@Override
	public ResponseEntity<ConcurrentProcessLogsInformation> findAllCompleteProcessesForBatchId(String batchId) {

		log.debug("findAllCompleteProcessesForBatchId called for batchId -> {}", batchId);

		return prepareResponse(
				concurrentProcessLogRepository.findAllByBatchIdAndProcessEndDateTimeIsNotNull(UUID.fromString(batchId)),
				batchId);
	}

	@Override
	public ResponseEntity<ConcurrentProcessLogDefinition> findProcessByProcessId(String processId) {

		log.debug("findProcessByProcessId called for processId -> {}", processId);

		return concurrentProcessLogRepository.findById(UUID.fromString(processId)).map(processLog -> {

			return new ResponseEntity<ConcurrentProcessLogDefinition>(
					concurrentProcessLogTransformer.tranformToConcurrentProcessLogResponse(processLog), HttpStatus.OK);
		}).orElseThrow(() -> new ResourceNotFoundException(
				"Requested Concurrent Process not found for processId# " + processId));
	}

	private ResponseEntity<ConcurrentProcessLogsInformation> prepareResponse(
			Iterable<CoreConcurrentProcessLog> coreConcurrentProcessLogIterable, String batchId) {

		log.debug("prepareResponse called for batchId -> {}", batchId);

		if (IterableUtil.isNullOrEmpty(coreConcurrentProcessLogIterable)) {

			if (null != batchId) {

				log.error("CoreConcurrentProcessLogIterable is null, No process exists for batchId# {}", batchId);
				throw new ResourceNotFoundException("No process exists for batchId# " + batchId);
			}
		}

		List<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitionList = new ArrayList<ConcurrentProcessLogDefinition>();
		coreConcurrentProcessLogIterable.forEach(coreConcurrentProcessLog -> {

			concurrentProcessLogDefinitionList.add(
					concurrentProcessLogTransformer.tranformToConcurrentProcessLogResponse(coreConcurrentProcessLog));
		});

		ConcurrentProcessLogsInformation concurrentProcessLogsInformation = new ConcurrentProcessLogsInformation();
		concurrentProcessLogsInformation.setConcurrentProcessLogDefinitions(concurrentProcessLogDefinitionList);

		if (null != concurrentProcessLogDefinitionList && !concurrentProcessLogDefinitionList.isEmpty()) {

			concurrentProcessLogsInformation
					.setTotalProcessesCount(BigInteger.valueOf(concurrentProcessLogDefinitionList.size()));
		}

		return new ResponseEntity<ConcurrentProcessLogsInformation>(concurrentProcessLogsInformation, HttpStatus.OK);
	}

}