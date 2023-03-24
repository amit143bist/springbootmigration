package com.ds.migration.feign.coredata.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogsInformation;

public interface CoreProcessFailureLogService {

	@PostMapping("/migration/scheduledbatch/concurrentprocessfailure")
	ResponseEntity<ConcurrentProcessFailureLogDefinition> saveFailureLog(
			@RequestBody ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition);

	@PutMapping("/migration/scheduledbatch/concurrentprocessfailure/processes/{processFailureId}")
	ResponseEntity<ConcurrentProcessFailureLogDefinition> updateFailureLog(
			@RequestBody ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition,
			@PathVariable String processFailureId);

	@GetMapping("/migration/scheduledbatch/concurrentprocessfailure")
	ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLog();

	@GetMapping("/migration/scheduledbatch/concurrentprocessfailure/processes/{processId}")
	ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLogForConcurrentProcessId(
			@PathVariable String processId);

	@GetMapping("/migration/scheduledbatch/concurrentprocessfailure/failurerecords/{failureRecordId}")
	ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailureLogForFailureRecordId(
			@PathVariable String failureRecordId);

	@PutMapping("/migration/scheduledbatch/concurrentprocessfailure/failurerecords")
	ResponseEntity<ConcurrentProcessFailureLogsInformation> listAllProcessFailuresByProcessIds(
			@RequestBody List<UUID> processIds);

	@PutMapping("/migration/scheduledbatch/concurrentprocessfailure/failurerecords/processids/count")
	ResponseEntity<Long> countProcessFailuresByProcessIds(@RequestBody List<UUID> processIds);

	@GetMapping("/migration/scheduledbatch/concurrentprocessfailure/failurerecords/count")
	ResponseEntity<Long> countProcessFailures();

}