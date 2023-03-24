package com.ds.migration.feign.website.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"aud", "expunit", "expdelta" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IbisTokenRequest implements MigrationInformation {

	@JsonProperty("aud")
	private String aud;
	@JsonProperty("expunit")
	private String expunit;
	@JsonProperty("expdelta")
	private Integer expdelta;

}