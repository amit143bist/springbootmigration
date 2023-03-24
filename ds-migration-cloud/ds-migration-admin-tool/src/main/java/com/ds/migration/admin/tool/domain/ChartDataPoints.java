package com.ds.migration.admin.tool.domain;

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
@JsonPropertyOrder({ "dataLabel", "dataLegendText", "dataShareValue" })
public class ChartDataPoints {

	@JsonProperty("dataLabel")
	private String dataLabel;
	@JsonProperty("dataLegendText")
	private String dataLegendText;
	@JsonProperty("dataShareValue")
	private Integer dataShareValue;

}