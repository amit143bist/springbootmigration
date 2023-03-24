package com.ds.migration.feign.process.domain;

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
@JsonPropertyOrder({ "envelopeId", "uri", "statusDateTime", "status" })
public class EnvelopeResponse implements MigrationInformation {

	@JsonProperty("envelopeId")
	private String envelopeId;
	@JsonProperty("uri")
	private String uri;
	@JsonProperty("statusDateTime")
	private String statusDateTime;
	@JsonProperty("status")
	private String status;

}