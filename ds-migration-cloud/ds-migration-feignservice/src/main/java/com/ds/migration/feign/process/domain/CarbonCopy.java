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
@JsonPropertyOrder({ "email", "name", "recipientId", "routingOrder" })
public class CarbonCopy implements MigrationInformation {

	@JsonProperty("email")
	private String email;
	@JsonProperty("name")
	private String name;
	@JsonProperty("recipientId")
	private String recipientId;
	@JsonProperty("routingOrder")
	private String routingOrder;

}