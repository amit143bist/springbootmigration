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
@JsonPropertyOrder({ "recordId", "docuSignId" })
public class MigrationRecordIdInformation implements MigrationInformation {

	@JsonProperty("recordId")
	private String recordId;
	@JsonProperty("docuSignId")
	private String docuSignId;
}