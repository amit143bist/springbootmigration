package com.ds.migration.processcomplete.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;
import com.ds.migration.feign.coredata.service.CoreConcurrentProcessLogService;

@FeignClient(contextId="coreConcurrentProcessLogClient", value="dsmigrationcoredata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationcoredata", configuration = RibbonConfiguration.class)
public interface CoreConcurrentProcessLogClient extends CoreConcurrentProcessLogService {

}