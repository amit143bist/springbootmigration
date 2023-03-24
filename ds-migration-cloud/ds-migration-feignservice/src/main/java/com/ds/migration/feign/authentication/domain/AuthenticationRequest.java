package com.ds.migration.feign.authentication.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "user", "scopes" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest implements MigrationInformation {

	@JsonProperty("user")
	private String user;
	@JsonProperty("scopes")
	private String scopes;

}