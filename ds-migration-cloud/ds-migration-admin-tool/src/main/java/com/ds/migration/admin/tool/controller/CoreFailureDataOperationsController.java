package com.ds.migration.admin.tool.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.admin.tool.client.CoreConcurrentProcessLogClient;
import com.ds.migration.admin.tool.client.CoreProcessFailureLogClient;
import com.ds.migration.admin.tool.client.CoreScheduledBatchLogClient;
import com.ds.migration.admin.tool.service.SendToProcessStartService;
import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogsInformation;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CoreFailureDataOperationsController {

	@Autowired
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@Autowired
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@Autowired
	CoreProcessFailureLogClient coreProcessFailureLogClient;

	@Autowired
	SendToProcessStartService sendToProcessStartService;

	@ApiOperation(value = "Get list of Failed Messages for the batchId", notes = "A BatchId is needed to get list of failed messages")
	@GetMapping("/migration/tool/listallfailedmessages/batchid/{batchId}")
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllFailedMessagesForBatch(
			@PathVariable String batchId) {

		return fetchAllFailedMessagesFromProcessIds(batchId);
	}

	@ApiOperation(value = "Get list of Failed Messages for the latest batch of a batchType", notes = "A [batchType] is needed to get list of failed messages, PRONTOMIGRATION is the value for migration related to Pronto")
	@GetMapping("/migration/tool/listallfailedmessages/latestbatch/batchtype/{batchType}")
	public ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllFailedMessagesForLatestBatch(
			@ApiParam(defaultValue = "PRONTOMIGRATION") @PathVariable String batchType) {

		ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponse = coreScheduledBatchLogClient
				.findLatestBatchByBatchType(batchType.toUpperCase());

		return fetchAllFailedMessagesFromProcessIds(scheduledBatchLogResponse.getBody().getBatchId());
	}

	private ResponseEntity<ConcurrentProcessFailureLogsInformation> fetchAllFailedMessagesFromProcessIds(
			String batchId) {

		ConcurrentProcessLogsInformation processInformation = coreConcurrentProcessLogClient
				.findAllInCompleteProcessesForBatchId(batchId).getBody();

		List<UUID> processUUIDList = prepareProcessUUIDList(processInformation);

		if (null != processUUIDList && !processUUIDList.isEmpty()) {

			return new ResponseEntity<ConcurrentProcessFailureLogsInformation>(
					coreProcessFailureLogClient.listAllProcessFailuresByProcessIds(processUUIDList).getBody(),
					HttpStatus.OK);
		} else {

			return new ResponseEntity<ConcurrentProcessFailureLogsInformation>(HttpStatus.NOT_FOUND);
		}

	}

	private List<UUID> prepareProcessUUIDList(ConcurrentProcessLogsInformation processInformation) {

		List<UUID> processUUIDList = new ArrayList<UUID>();

		for (ConcurrentProcessLogDefinition concurrentProcessLogDefinition : processInformation
				.getConcurrentProcessLogDefinitions()) {

			processUUIDList.add(UUID.fromString(concurrentProcessLogDefinition.getProcessId()));
		}

		return processUUIDList;
	}

	@ApiOperation(value = "Retry list of Failed Messages for the batchId", notes = "A BatchId is needed to retry list of failed messages")
	@PutMapping("/migration/tool/retryallfailedmessages/batches/{batchId}")
	public ResponseEntity<String> retryAllFailedMessagesByBatchId(@PathVariable String batchId) {

		log.info("BatchId in retryAllFailedMessagesByBatch is {} ", batchId);

		return findAllFailureMessageByBatchId(batchId);

	}

	@ApiOperation(value = "Retry list of Failed Messages for the batchtype", notes = "A [batchType] is needed to retry list of failed messages, PRONTOMIGRATION is the value for migration related to Pronto")
	@PutMapping("/migration/tool/retryallfailedmessages/latestbatch/batchtype/{batchType}")
	public ResponseEntity<String> retryAllFailedMessagesForLatestBatch(
			@ApiParam(defaultValue = "PRONTOMIGRATION") @PathVariable String batchType) {

		ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponseEntity = coreScheduledBatchLogClient
				.findLatestBatchByBatchType(batchType.toUpperCase());

		if (null != scheduledBatchLogResponseEntity
				&& HttpStatus.NOT_FOUND != scheduledBatchLogResponseEntity.getStatusCode()) {

			log.info("BatchId in retryAllFailedMessagesForLatestBatch for latestBatchType {} is {} ",
					batchType.toUpperCase(), scheduledBatchLogResponseEntity.getBody().getBatchId());

			return findAllFailureMessageByBatchId(scheduledBatchLogResponseEntity.getBody().getBatchId());

		} else {

			log.error("No Batch exist for batchType -> {} in retryAllFailedMessagesForLatestBatch",
					batchType.toUpperCase());
			return new ResponseEntity<String>("No Batch exist for batchType -> " + batchType.toUpperCase(),
					HttpStatus.NOT_FOUND);
		}

	}

	@ApiOperation(value = "Get list of Failed Messages for the failureRecordId/signeddraftId", notes = "A failureRecordId/signeddraftId is needed to retry list of failed messages")
	@PutMapping("/migration/tool/retryfailedmessages/failures/{failureRecordId}")
	public ResponseEntity<String> retryAllFailedMessagesByFailureRecordId(@PathVariable String failureRecordId) {

		log.info("FailureRecordId in retryAllFailedMessagesByFailureRecordId is {} ", failureRecordId);

		ResponseEntity<ConcurrentProcessFailureLogsInformation> concurrentProcessFailureLogsInformationRespEntity = coreProcessFailureLogClient
				.listAllProcessFailureLogForFailureRecordId(failureRecordId);

		if (null != concurrentProcessFailureLogsInformationRespEntity
				&& HttpStatus.NOT_FOUND != concurrentProcessFailureLogsInformationRespEntity.getStatusCode()) {

			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = concurrentProcessFailureLogsInformationRespEntity
					.getBody().getConcurrentProcessFailureLogDefinitions().get(0);

			ResponseEntity<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitionEntity = coreConcurrentProcessLogClient
					.findProcessByProcessId(concurrentProcessFailureLogDefinition.getProcessId());

			return processAllFailureEntries(concurrentProcessLogDefinitionEntity.getBody().getBatchId(),
					concurrentProcessFailureLogsInformationRespEntity);
		} else {

			return new ResponseEntity<String>("No Failure entries exist for failureRecordId -> " + failureRecordId,
					HttpStatus.NOT_FOUND);

		}

	}

	@ApiOperation(value = "Get list of Failed Messages for the processId", notes = "A processId is needed to retry list of failed messages")
	@PutMapping("/migration/tool/retryfailedmessages/processes/{processId}")
	public ResponseEntity<String> retryAllFailedMessagesByProcessId(@PathVariable String processId) {

		log.info("FailureRecordId in retryAllFailedMessagesByProcessId is {} ", processId);

		return processAllFailureEntries(
				coreConcurrentProcessLogClient.findProcessByProcessId(processId).getBody().getBatchId(),
				coreProcessFailureLogClient.listAllProcessFailureLogForConcurrentProcessId(processId));

	}

	private ResponseEntity<String> findAllFailureMessageByBatchId(String batchId) {

		ResponseEntity<ConcurrentProcessFailureLogsInformation> concurrentProcessFailureLogsInformationRespEntity = fetchAllFailedMessagesFromProcessIds(
				batchId);

		return processAllFailureEntries(batchId, concurrentProcessFailureLogsInformationRespEntity);
	}

	private ResponseEntity<String> processAllFailureEntries(String batchId,
			ResponseEntity<ConcurrentProcessFailureLogsInformation> concurrentProcessFailureLogsInformationRespEntity) {

		if (HttpStatus.NOT_FOUND != concurrentProcessFailureLogsInformationRespEntity.getStatusCode()) {

			ConcurrentProcessFailureLogsInformation concurrentProcessFailureLogsInformation = concurrentProcessFailureLogsInformationRespEntity
					.getBody();

			if (null != concurrentProcessFailureLogsInformation
					&& null != concurrentProcessFailureLogsInformation.getConcurrentProcessFailureLogDefinitions()
					&& !concurrentProcessFailureLogsInformation.getConcurrentProcessFailureLogDefinitions().isEmpty()) {

				log.info("Found all failure records in findAllFailureMessageByBatchId for batchId {}", batchId);

				List<ConcurrentProcessFailureLogDefinition> concurrentProcessFailureLogDefinitionList = concurrentProcessFailureLogsInformation
						.getConcurrentProcessFailureLogDefinitions();

				Map<String, List<BigInteger>> processRecordIdsMap = new HashMap<String, List<BigInteger>>();
				Map<String, String> failureIdProcessFailureIdMap = new HashMap<String, String>();

				for (ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition : concurrentProcessFailureLogDefinitionList) {

					prepareProcessMap(processRecordIdsMap, concurrentProcessFailureLogDefinition,
							failureIdProcessFailureIdMap);
				}

				processMapToProcessStartMessage(batchId, processRecordIdsMap, failureIdProcessFailureIdMap);
			}

			return new ResponseEntity<String>(ValidationResult.SUCCESS.toString(), HttpStatus.OK);
		} else {

			return new ResponseEntity<String>("No Failure message exist for batchId -> " + batchId,
					HttpStatus.NOT_FOUND);
		}
	}

	private void processMapToProcessStartMessage(String batchId, Map<String, List<BigInteger>> processRecordIdsMap,
			Map<String, String> failureIdProcessFailureIdMap) {

		if (processRecordIdsMap.size() > 0) {

			processRecordIdsMap.entrySet();
			for (Entry<String, List<BigInteger>> mapEntry : processRecordIdsMap.entrySet()) {

				String processId = mapEntry.getKey();
				ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition = new ConcurrentProcessMessageDefinition();
				concurrentProcessMessageDefinition.setBatchId(batchId);
				concurrentProcessMessageDefinition.setProcessId(processId);
				concurrentProcessMessageDefinition.setSignedDraftIds(mapEntry.getValue());
				concurrentProcessMessageDefinition.setFailureIdProcessFailureIdMap(failureIdProcessFailureIdMap);

				sendToProcessStartService.sendProcessStartMessage(concurrentProcessMessageDefinition);
			}
		}
	}

	private void prepareProcessMap(Map<String, List<BigInteger>> processRecordIdsMap,
			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition,
			Map<String, String> failureIdProcessFailureIdMap) {

		String processId = concurrentProcessFailureLogDefinition.getProcessId();

		if (null != processRecordIdsMap.get(processId)) {

			List<BigInteger> failureRecordIds = processRecordIdsMap.get(processId);
			failureRecordIds.add(new BigInteger(concurrentProcessFailureLogDefinition.getFailureRecordId()));

			List<BigInteger> failureRecordIdsWithoutDuplicates = failureRecordIds.stream().distinct()
					.collect(Collectors.toList());
			processRecordIdsMap.put(processId, failureRecordIdsWithoutDuplicates);

		} else {

			List<BigInteger> failureRecordIds = new ArrayList<BigInteger>();
			failureRecordIds.add(new BigInteger(concurrentProcessFailureLogDefinition.getFailureRecordId()));
			processRecordIdsMap.put(processId, failureRecordIds);
		}

		failureIdProcessFailureIdMap.put(concurrentProcessFailureLogDefinition.getFailureRecordId(),
				concurrentProcessFailureLogDefinition.getProcessFailureId());
	}
}