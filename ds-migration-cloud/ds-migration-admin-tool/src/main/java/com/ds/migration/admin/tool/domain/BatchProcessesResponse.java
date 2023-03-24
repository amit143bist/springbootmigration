package com.ds.migration.admin.tool.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "totalCompleteProcess", "totalInCompleteProcess", "completeProcessIds", "inCompleteProcessIds" })
public class BatchProcessesResponse {

	@JsonProperty("totalCompleteProcess")
	private Integer totalCompleteProcess;
	@JsonProperty("totalInCompleteProcess")
	private Integer totalInCompleteProcess;
	@JsonProperty("completeProcessIds")
	private List<String> completeProcessIds;
	@JsonProperty("inCompleteProcessIds")
	private List<String> inCompleteProcessIds;
	
}