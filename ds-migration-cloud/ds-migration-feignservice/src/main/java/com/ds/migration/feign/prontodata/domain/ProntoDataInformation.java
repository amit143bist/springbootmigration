package com.ds.migration.feign.prontodata.domain;

import java.util.Map;

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
@JsonPropertyOrder({ "dataStatus", "prontoDataSignedDraftResponseMap" })
public class ProntoDataInformation implements MigrationInformation {

	@JsonProperty("dataStatus")
	private String dataStatus = null;
	@JsonProperty("prontoDataSignedDraftResponseMap")
	private Map<Long, MigrationProntoDataResponse[]> prontoDataSignedDraftResponseMap = null;

}