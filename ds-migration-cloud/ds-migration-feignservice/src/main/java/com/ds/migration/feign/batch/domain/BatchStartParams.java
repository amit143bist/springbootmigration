package com.ds.migration.feign.batch.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStartParams {
	
	private String beginDateTime;
	private String endDateTime;
	private Integer totalRecordsPerProcess;
}