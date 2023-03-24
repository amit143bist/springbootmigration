package com.ds.migration.feign.batch.domain;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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
@JsonPropertyOrder({ "batchId", "processId", "signedDraftIds", "failureIdProcessFailureIdMap" })
public class ConcurrentProcessMessageDefinition implements MigrationInformation {

	@JsonProperty("batchId")
	private String batchId;
	@JsonProperty("processId")
	private String processId;
	@JsonProperty("signedDraftIds")
	private List<BigInteger> signedDraftIds = null;
	@JsonProperty("failureIdProcessFailureIdMap")
	private Map<String, String> failureIdProcessFailureIdMap;

}