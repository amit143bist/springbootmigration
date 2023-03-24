package com.ds.migration.recorddata.consumer.client;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;

import com.ds.migration.feign.auditdata.service.MigrationRecordIdService;
import com.ds.migration.feign.config.FeignClientConfiguration;
import com.ds.migration.feign.config.RibbonConfiguration;

@FeignClient(value="dsmigrationauditdata", configuration = FeignClientConfiguration.class)
@RibbonClient(name = "dsmigrationauditdata", configuration = RibbonConfiguration.class)
public interface MigrationRecordIdClient extends MigrationRecordIdService {

}