package com.ds.migration.feign.website.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "attachmentId", "label", "attachmentType", "name", "accessControl", "errorDetails" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Attachment implements MigrationInformation {

	@JsonProperty("attachmentId")
	private String attachmentId;
	@JsonProperty("label")
	private String label;
	@JsonProperty("attachmentType")
	private String attachmentType;
	@JsonProperty("name")
	private String name;
	@JsonProperty("accessControl")
	private String accessControl;
	@JsonProperty("errorDetails")
	private ErrorDetails errorDetails;

}