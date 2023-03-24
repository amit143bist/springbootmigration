package com.ds.migration.feign.auditdata.domain;

import java.math.BigInteger;
import java.util.List;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "processId", "totalRecords", "migrationAuditDataRequestList" })
public class MigrationAuditDataDefinition implements MigrationInformation {

	private String processId;
	private BigInteger totalRecords;
	private List<MigrationAuditDataRequest> migrationAuditDataRequestList;
}