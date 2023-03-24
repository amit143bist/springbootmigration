package com.ds.migration.feign.batch.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "batchType", "batchStartDateTime", "batchEndDateTime", "numberOfHours",
		"numberOfRecordsPerThread" })
public class MigrationBatchTriggerInformation implements MigrationInformation {

	@JsonProperty("batchType")
	private String batchType;
	@JsonProperty("batchStartDateTime")
	private String batchStartDateTime;
	@JsonProperty("batchEndDateTime")
	private String batchEndDateTime;
	@JsonProperty("numberOfHours")
	private Integer numberOfHours;
	@JsonProperty("numberOfRecordsPerThread")
	private Integer numberOfRecordsPerThread;
}