package com.ds.migration.batchtrigger.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;
import com.ds.migration.feign.coredata.service.CoreScheduledBatchLogService;

@FeignClient(contextId="coreScheduledBatchLogClient", value="dsmigrationcoredata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationcoredata", configuration = RibbonConfiguration.class)
public interface CoreScheduledBatchLogClient extends CoreScheduledBatchLogService {

}