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
@JsonPropertyOrder({ "processFailureId", "processId", "failureCode", "failureReason", "failureDateTime",
		"successDateTime", "failureRecordId", "failureStep", "retryStatus", "retryCount" })
public class ConcurrentProcessFailureLogDefinition implements MigrationInformation {

	@JsonProperty("processFailureId")
	private String processFailureId;
	@JsonProperty("processId")
	private String processId;
	@JsonProperty("failureCode")
	private String failureCode;
	@JsonProperty("failureReason")
	private String failureReason;
	@JsonProperty("failureDateTime")
	private String failureDateTime;
	@JsonProperty("successDateTime")
	private String successDateTime;
	@JsonProperty("failureRecordId")
	private String failureRecordId;
	@JsonProperty("failureStep")
	private String failureStep;
	@JsonProperty("retryStatus")
	private String retryStatus;
	@JsonProperty("retryCount")
	private BigInteger retryCount;

}