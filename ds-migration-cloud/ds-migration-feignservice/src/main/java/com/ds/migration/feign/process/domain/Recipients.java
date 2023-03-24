package com.ds.migration.feign.process.domain;

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
@JsonPropertyOrder({ "carbonCopies" })
public class Recipients implements MigrationInformation {

	@JsonProperty("carbonCopies")
	private List<CarbonCopy> carbonCopies = null;

}