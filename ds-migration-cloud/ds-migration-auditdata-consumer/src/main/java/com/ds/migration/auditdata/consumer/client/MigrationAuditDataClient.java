package com.ds.migration.auditdata.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.auditdata.service.MigrationAuditDataService;
import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;

@FeignClient(value="dsmigrationauditdata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationauditdata", configuration = RibbonConfiguration.class)
public interface MigrationAuditDataClient extends MigrationAuditDataService {

}