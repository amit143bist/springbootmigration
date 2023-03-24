package com.ds.migration.feign.coredata.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogsInformation;

public interface CoreScheduledBatchLogService {

	@PostMapping("/migration/scheduledbatch")
	ResponseEntity<ScheduledBatchLogResponse> saveBatch(@RequestBody ScheduledBatchLogRequest scheduledBatchLogRequest);

	@PutMapping("/migration/scheduledbatch/{batchId}")
	ResponseEntity<ScheduledBatchLogResponse> updateBatch(@PathVariable String batchId);

	@GetMapping("/migration/scheduledbatch/batchtype/{batchType}")
	ResponseEntity<ScheduledBatchLogResponse> findInCompleteBatch(@PathVariable String batchType);

	@GetMapping("/migration/scheduledbatch/incompletebatches/batchtype/{batchType}")
	ResponseEntity<ScheduledBatchLogsInformation> findAllInCompleteBatches(@PathVariable String batchType);

	@GetMapping("/migration/scheduledbatch/completebatches/batchtype/{batchType}")
	ResponseEntity<ScheduledBatchLogsInformation> findAllCompleteBatches(@PathVariable String batchType);

	@GetMapping("/migration/scheduledbatch/batches/batchtype/{batchType}")
	ResponseEntity<ScheduledBatchLogsInformation> findAllBatchesByBatchType(@PathVariable String batchType);

	@GetMapping("/migration/scheduledbatch/batches/batchtype/{batchType}/fromdate/{fromDate}/todate/{toDate}")
	ResponseEntity<ScheduledBatchLogsInformation> findAllByBatchTypeAndBatchStartDateTimeBetween(
			@PathVariable String batchType, @PathVariable String fromDate, @PathVariable String toDate);

	@GetMapping("/migration/scheduledbatch/batches")
	ResponseEntity<ScheduledBatchLogsInformation> findAllBatches();

	@GetMapping("/migration/scheduledbatch/latestbatch/batchid/{batchId}")
	ResponseEntity<ScheduledBatchLogResponse> findBatchByBatchId(@PathVariable String batchId);

	@GetMapping("/migration/scheduledbatch/latestbatch/batchtype/{batchType}")
	ResponseEntity<ScheduledBatchLogResponse> findLatestBatchByBatchType(@PathVariable String batchType);

}