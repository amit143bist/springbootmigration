package com.ds.migration.feign.coredata.domain;

import java.math.BigInteger;
import java.util.List;

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
@JsonPropertyOrder({ "totalBatchesCount", "scheduledBatchLogResponses" })
public class ScheduledBatchLogsInformation implements MigrationInformation {

	@JsonProperty("totalBatchesCount")
	private BigInteger totalBatchesCount;
	@JsonProperty("scheduledBatchLogResponses")
	private List<ScheduledBatchLogResponse> scheduledBatchLogResponses = null;

}