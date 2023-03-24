package com.ds.migration.processstart.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.authentication.service.MigrationAuthenticationService;
import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;

@FeignClient(value = "dsmigrationauthentication", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationauthentication", configuration = RibbonConfiguration.class)
public interface MigrationAuthenticationClient extends MigrationAuthenticationService {

}