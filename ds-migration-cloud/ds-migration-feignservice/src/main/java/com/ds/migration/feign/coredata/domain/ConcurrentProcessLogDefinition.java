package com.ds.migration.feign.coredata.domain;

import java.math.BigInteger;

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
@JsonPropertyOrder({ "processId", "batchId", "processStartDateTime", "processEndDateTime", "processStatus",
		"totalRecordsInProcess" })
public class ConcurrentProcessLogDefinition implements MigrationInformation {

	@JsonProperty("processId")
	private String processId;
	@JsonProperty("batchId")
	private String batchId;
	@JsonProperty("processStartDateTime")
	private String processStartDateTime;
	@JsonProperty("processEndDateTime")
	private String processEndDateTime;
	@JsonProperty("processStatus")
	private String processStatus;
	@JsonProperty("totalRecordsInProcess")
	private BigInteger totalRecordsInProcess;

}