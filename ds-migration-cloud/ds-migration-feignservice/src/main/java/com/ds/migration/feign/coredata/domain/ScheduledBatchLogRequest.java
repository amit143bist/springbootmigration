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
@JsonPropertyOrder({ "batchType", "batchStartParameters", "totalRecords" })
public class ScheduledBatchLogRequest implements MigrationInformation {

	@JsonProperty("batchType")
	private String batchType;
	@JsonProperty("batchStartParameters")
	private String batchStartParameters;
	@JsonProperty("totalRecords")
	private BigInteger totalRecords;

}