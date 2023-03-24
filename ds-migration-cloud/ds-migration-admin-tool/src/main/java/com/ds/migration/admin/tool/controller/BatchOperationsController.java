package com.ds.migration.admin.tool.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.admin.tool.client.CoreScheduledBatchLogClient;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogsInformation;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class BatchOperationsController {

	@Autowired
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@ApiOperation(value = "Get Last Batch details for a batchType", notes = "Get Last Batch details for a batchType")
	@GetMapping("/migration/tool/latestbatch/batchtype/{batchType}")
	public ResponseEntity<ScheduledBatchLogResponse> fetchLastBatchDetails(
			@ApiParam(defaultValue = "PRONTOMIGRATION") @PathVariable String batchType) {

		ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponse = coreScheduledBatchLogClient
				.findLatestBatchByBatchType(batchType.toUpperCase());

		return new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse.getBody(), HttpStatus.OK);

	}

	@ApiOperation(value = "Get Last Batch details for a batchId", notes = "Get Last Batch details for a batchId")
	@GetMapping("/migration/tool/batches/batchid/{batchId}")
	public ResponseEntity<ScheduledBatchLogResponse> fetchBatchDetailsByBatchId(@PathVariable String batchId) {

		ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponse = coreScheduledBatchLogClient
				.findBatchByBatchId(batchId);

		return new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse.getBody(), HttpStatus.OK);

	}

	@ApiOperation(value = "Get all batches for a batchType", notes = "Get all batches for a batchType")
	@GetMapping("/migration/tool/allbatches/batchtype/{batchType}")
	public ResponseEntity<ScheduledBatchLogsInformation> fetchAllBatchesByBatchType(
			@ApiParam(defaultValue = "PRONTOMIGRATION") @PathVariable String batchType) {

		ResponseEntity<ScheduledBatchLogsInformation> scheduledBatchLogsInformationResponse = coreScheduledBatchLogClient
				.findAllBatchesByBatchType(batchType.toUpperCase());

		return new ResponseEntity<ScheduledBatchLogsInformation>(scheduledBatchLogsInformationResponse.getBody(),
				HttpStatus.OK);

	}

	@ApiOperation(value = "Close a hung batch job", notes = "Close a hung batch job")
	@PutMapping("/migration/tool/batches/hungbatchid/{batchId}")
	public ResponseEntity<ScheduledBatchLogResponse> closeHungBatch(@PathVariable String batchId) {

		ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponse = coreScheduledBatchLogClient
				.updateBatch(batchId);

		return new ResponseEntity<ScheduledBatchLogResponse>(scheduledBatchLogResponse.getBody(), HttpStatus.OK);

	}

	@ApiOperation(value = "Get Batches by date range", notes = "Get Batches by date range")
	@GetMapping("/migration/tool/batches/batchtype/{batchType}/fromdate/{fromDate}/todate/{toDate}")
	public ResponseEntity<ScheduledBatchLogsInformation> fetcBatchDetailsByDateRange(
			@ApiParam(defaultValue = "PRONTOMIGRATION") @PathVariable String batchType,
			@ApiParam(value = "From Date, yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss.SSS format") @PathVariable String fromDate,
			@ApiParam(value = "To Date, yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss.SSS format") @PathVariable String toDate) {

		log.info("In fetchBatchDetailsByDateRange() fromDate -> {}, toDate -> {}", fromDate, toDate);

		if (!fromDate.contains("T")) {

			fromDate = fromDate + "T00:00:00.000";
		}

		if (!toDate.contains("T")) {

			toDate = toDate + "T23:59:59.999";
		}

		if (MigrationDateTimeUtil.isValidDateTime(fromDate) && MigrationDateTimeUtil.isValidDateTime(toDate)) {

			ResponseEntity<ScheduledBatchLogsInformation> scheduledBatchLogsInformationResponse = coreScheduledBatchLogClient
					.findAllByBatchTypeAndBatchStartDateTimeBetween(batchType.toUpperCase(), fromDate, toDate);

			return new ResponseEntity<ScheduledBatchLogsInformation>(scheduledBatchLogsInformationResponse.getBody(),
					HttpStatus.OK);
		} else {

			log.error(
					"Either fromDate {} and/or toDate {} are/is not in acceptable format, acceptable format is yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss.SSS",
					fromDate, toDate);
			return new ResponseEntity<ScheduledBatchLogsInformation>(HttpStatus.BAD_REQUEST);
		}

	}
}