package com.ds.migration.admin.tool.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.admin.tool.client.CoreConcurrentProcessLogClient;
import com.ds.migration.admin.tool.domain.BatchProcessesResponse;
import com.ds.migration.admin.tool.domain.MigrationToolInformation;
import com.ds.migration.admin.tool.service.SendToProcessCompleteService;
import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class CoreProcessDataOperationsController extends AbstractOperationsController {

	@Autowired
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@Autowired
	SendToProcessCompleteService sendToProcessCompleteService;

	@ApiOperation(value = "Get details of a processId", notes = "A processId is needed to get the details")
	@GetMapping("/migration/tool/processes/processid/{processId}")
	public ResponseEntity<ConcurrentProcessLogDefinition> findProcessDetails(@PathVariable String processId) {

		log.info("findProcessDetails called for processId -> {}", processId);
		ConcurrentProcessLogDefinition fetchedConcurrentProcessLogDefinition = coreConcurrentProcessLogClient
				.findProcessByProcessId(processId).getBody();

		return new ResponseEntity<ConcurrentProcessLogDefinition>(fetchedConcurrentProcessLogDefinition, HttpStatus.OK);

	}

	@ApiOperation(value = "Close a hung process", notes = "A hung or failed processId can be closed with this operation by passing processId")
	@PutMapping("/migration/tool/processes/hungprocessid/{processId}")
	public void closeHungProcess(@PathVariable String processId) {

		log.info("closeHungProcess called for processId -> {}", processId);
		ConcurrentProcessLogDefinition fetchedConcurrentProcessLogDefinition = coreConcurrentProcessLogClient
				.findProcessByProcessId(processId).getBody();

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(fetchedConcurrentProcessLogDefinition.getBatchId());
		concurrentProcessLogDefinition.setProcessId(processId);
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.MANUALLY_CLOSED.toString());

		sendToProcessCompleteService.sendProcessCompleteMessage(concurrentProcessLogDefinition);

	}

	@ApiOperation(value = "Get all processes status of a batch", notes = "A batchId is required to see the status of all associated processes")
	@GetMapping("/migration/tool/processes/batchid/{batchId}")
	public ResponseEntity<BatchProcessesResponse> fetchAllStatusProcesses(@PathVariable String batchId) {

		ConcurrentProcessLogsInformation processInformation = coreConcurrentProcessLogClient
				.findAllProcessesForBatchId(batchId).getBody();

		List<String> completedProcesses = new ArrayList<String>();
		List<String> inCompletedProcesses = new ArrayList<String>();

		List<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitionList = processInformation
				.getConcurrentProcessLogDefinitions();

		for (ConcurrentProcessLogDefinition concurrentProcessLogDefinition : concurrentProcessLogDefinitionList) {

			if (ProcessStatus.INPROGRESS.toString()
					.equalsIgnoreCase(concurrentProcessLogDefinition.getProcessStatus())) {
				inCompletedProcesses.add(concurrentProcessLogDefinition.getProcessId());
			}

			if (ProcessStatus.COMPLETED.toString()
					.equalsIgnoreCase(concurrentProcessLogDefinition.getProcessStatus())) {
				completedProcesses.add(concurrentProcessLogDefinition.getProcessId());
			}
		}

		BatchProcessesResponse batchProcessesResponse = new BatchProcessesResponse();
		batchProcessesResponse.setTotalCompleteProcess(completedProcesses.size());
		batchProcessesResponse.setTotalInCompleteProcess(inCompletedProcesses.size());
		batchProcessesResponse.setCompleteProcessIds(completedProcesses);
		batchProcessesResponse.setInCompleteProcessIds(inCompletedProcesses);

		return new ResponseEntity<BatchProcessesResponse>(batchProcessesResponse, HttpStatus.OK);
	}

	@ApiOperation(value = "Get only Incomplete processes status of a batch", notes = "A batchId is required to see the details of incomplete processes")
	@GetMapping("/migration/tool/incompleteprocesses/batchid/{batchId}")
	public ResponseEntity<ConcurrentProcessLogsInformation> fetchInCompleteStatusProcesses(
			@PathVariable String batchId) {

		ConcurrentProcessLogsInformation processInformation = coreConcurrentProcessLogClient
				.findAllInCompleteProcessesForBatchId(batchId).getBody();

		return new ResponseEntity<ConcurrentProcessLogsInformation>(processInformation, HttpStatus.OK);
	}

	@ApiOperation(value = "Get only complete processes status of a batch", notes = "A batchId is required to see the details of complete processes")
	@GetMapping("/migration/tool/completeprocesses/batchid/{batchId}")
	public ResponseEntity<ConcurrentProcessLogsInformation> fetchCompleteStatusProcesses(@PathVariable String batchId) {

		ConcurrentProcessLogsInformation processInformation = coreConcurrentProcessLogClient
				.findAllCompleteProcessesForBatchId(batchId).getBody();

		return new ResponseEntity<ConcurrentProcessLogsInformation>(processInformation, HttpStatus.OK);
	}

	@ApiOperation(value = "Get Batch Velocity", notes = "Get Batch Velocity in 14 categories")
	@GetMapping("/migration/tool/currentbatchvelocity/batchid/{batchId}")
	public ResponseEntity<MigrationToolInformation> calculateCurrentBatchVelocity(@PathVariable String batchId) {

		ConcurrentProcessLogsInformation processInformation = coreConcurrentProcessLogClient
				.findAllCompleteProcessesForBatchId(batchId).getBody();

		List<ConcurrentProcessLogDefinition> concurrentProcessLogDefinitionList = processInformation
				.getConcurrentProcessLogDefinitions();

		Map<String, Integer> currentBatchVelocityMap = new LinkedHashMap<String, Integer>(14);

		for (ConcurrentProcessLogDefinition concurrentProcessLogDefinition : concurrentProcessLogDefinitionList) {

			Duration diffDuration = Duration.between(
					MigrationDateTimeUtil
							.convertToLocalDateTime(concurrentProcessLogDefinition.getProcessStartDateTime()),
					MigrationDateTimeUtil
							.convertToLocalDateTime(concurrentProcessLogDefinition.getProcessEndDateTime()));

			updateCurrentBatchVelocityMap(currentBatchVelocityMap, diffDuration.toMinutes(), diffDuration.toHours());

		}

		MigrationToolInformation migrationToolInformation = new MigrationToolInformation();
		migrationToolInformation.setCurrentBatchVelocityMap(convertToChartDataPoints(currentBatchVelocityMap));

		return new ResponseEntity<MigrationToolInformation>(migrationToolInformation, HttpStatus.OK);
	}

}