package com.ds.migration.prontodata.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;
import com.ds.migration.feign.prontodata.service.MigrationProntoDataService;

@FeignClient(value = "dsmigrationprontodata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationprontodata", configuration = RibbonConfiguration.class)
public interface MigrationProntoDataClient extends MigrationProntoDataService {

}