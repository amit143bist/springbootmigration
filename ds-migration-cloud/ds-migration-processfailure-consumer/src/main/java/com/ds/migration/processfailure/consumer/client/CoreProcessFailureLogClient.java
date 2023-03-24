package com.ds.migration.processfailure.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;
import com.ds.migration.feign.coredata.service.CoreProcessFailureLogService;

@FeignClient(value="dsmigrationcoredata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationcoredata", configuration = RibbonConfiguration.class)
public interface CoreProcessFailureLogClient extends CoreProcessFailureLogService {

}