package com.ds.migration.feign.auditdata.domain;

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
@JsonPropertyOrder({ "processId", "totalRecords", "migrationRecordIdInformationList" })
public class MigrationRecordIdInformationDefinition implements MigrationInformation {

	@JsonProperty("processId")
	private String processId;
	@JsonProperty("totalRecords")
	private BigInteger totalRecords;
	@JsonProperty("migrationRecordIdInformationList")
	private List<MigrationRecordIdInformation> migrationRecordIdInformationList;
}