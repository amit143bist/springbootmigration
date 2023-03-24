package com.ds.migration.feign.auditdata.domain;

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
@JsonPropertyOrder({ "auditId", "recordId", "processId", "auditEntryDateTime", "recordPhaseStatus", "recordPhase" })
public class MigrationAuditDataResponse implements MigrationInformation {

	@JsonProperty("auditId")
	private String auditId;
	@JsonProperty("recordId")
	private String recordId;
	@JsonProperty("processId")
	private String processId;
	@JsonProperty("auditEntryDateTime")
	private String auditEntryDateTime;
	@JsonProperty("recordPhaseStatus")
	private String recordPhaseStatus;
	@JsonProperty("recordPhase")
	private String recordPhase;

}