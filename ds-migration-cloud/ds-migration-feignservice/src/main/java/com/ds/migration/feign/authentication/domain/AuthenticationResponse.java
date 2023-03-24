package com.ds.migration.feign.authentication.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "error", "urlCodeGrant", "access_token", "token_type", "expires_in" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse implements MigrationInformation {

	@JsonProperty("error")
	private String error;
	@JsonProperty("urlCodeGrant")
	private String urlCodeGrant;
	@JsonProperty("access_token")
	private String accessToken;
	@JsonProperty("token_type")
	private String tokenType;
	@JsonProperty("expires_in")
	private Integer expiresIn;

}