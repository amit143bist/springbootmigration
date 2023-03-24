package com.ds.migration.feign.website.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errorCode", "message" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetails implements MigrationInformation {

	@JsonProperty("errorCode")
	private String errorCode;
	@JsonProperty("message")
	private String message;

}