package com.ds.migration.feign.prontodata.domain;

import java.util.List;

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
@JsonPropertyOrder({ "processId", "signedDraftIdList" })
public class MigrationSignedDraftDefinition {

	@JsonProperty("processId")
	private String processId = null;
	@JsonProperty("signedDraftIdList")
	private List<Long> signedDraftIdList = null;
}