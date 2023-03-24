package com.ds.migration.feign.auditdata.domain;

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
@JsonPropertyOrder({ "auditIds", "status" })
public class MigrationAuditValidationInformation implements MigrationInformation {

	@JsonProperty("auditIds")
	private List<String> auditIds = null;
	@JsonProperty("status")
	private String status = null;
}