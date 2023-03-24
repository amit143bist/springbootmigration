package com.ds.migration.feign.coredata.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogsInformation;

public interface CoreConcurrentProcessLogService {

	@PostMapping("/migration/scheduledbatch/concurrentprocess")
	ResponseEntity<ConcurrentProcessLogDefinition> saveConcurrentProcess(
			@RequestBody ConcurrentProcessLogDefinition concurrentProcessLogDefinition);

	@PutMapping("/migration/scheduledbatch/concurrentprocess/processes/{processId}")
	ResponseEntity<ConcurrentProcessLogDefinition> updateConcurrentProcess(
			@RequestBody ConcurrentProcessLogDefinition concurrentProcessLogDefinition, @PathVariable String processId);

	@GetMapping("/migration/scheduledbatch/concurrentprocess/countprocesses/{batchId}")
	ResponseEntity<Long> countPendingConcurrentProcessInBatch(@PathVariable String batchId);

	@GetMapping("/migration/scheduledbatch/concurrentprocess/processes/{batchId}")
	ResponseEntity<ConcurrentProcessLogsInformation> findAllProcessesForBatchId(@PathVariable String batchId);

	@GetMapping("/migration/scheduledbatch/concurrentprocess/process/{processId}")
	ResponseEntity<ConcurrentProcessLogDefinition> findProcessByProcessId(@PathVariable String processId);

	@GetMapping("/migration/scheduledbatch/concurrentprocess/incompleteprocesses/{batchId}")
	ResponseEntity<ConcurrentProcessLogsInformation> findAllInCompleteProcessesForBatchId(@PathVariable String batchId);

	@GetMapping("/migration/scheduledbatch/concurrentprocess/completeprocesses/{batchId}")
	ResponseEntity<ConcurrentProcessLogsInformation> findAllCompleteProcessesForBatchId(@PathVariable String batchId);

}