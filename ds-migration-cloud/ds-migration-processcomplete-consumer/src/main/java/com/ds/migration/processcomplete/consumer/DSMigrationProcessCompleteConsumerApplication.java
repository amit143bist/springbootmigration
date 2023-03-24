package com.ds.migration.processcomplete.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationProcessCompleteConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationProcessCompleteConsumerApplication.class, args);
	}

}