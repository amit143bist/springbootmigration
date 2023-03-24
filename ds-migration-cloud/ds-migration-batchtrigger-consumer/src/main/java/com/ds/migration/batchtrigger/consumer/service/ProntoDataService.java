package com.ds.migration.batchtrigger.consumer.service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ds.migration.batchtrigger.consumer.client.MigrationProntoDataClient;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.batch.domain.BatchStartParams;

@Service
public class ProntoDataService {

	@Autowired
	MigrationProntoDataClient migrationProntoDataClient;

	public List<BigInteger> fetchProntoData(BatchStartParams batchStartParams) {

		ResponseEntity<BigInteger[]> signedDraftIds = migrationProntoDataClient.signedDraftList(
				MigrationDateTimeUtil.convertToLocalDateTime(batchStartParams.getBeginDateTime()),
				MigrationDateTimeUtil.convertToLocalDateTime(batchStartParams.getEndDateTime()));

		
		return Arrays.asList(signedDraftIds.getBody());
	}
}