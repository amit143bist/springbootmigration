package com.ds.migration.feign.website.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "envelopeid" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnvelopeMappingResponse implements MigrationInformation {

	@JsonProperty("envelopeid")
	private String envelopeid;

}