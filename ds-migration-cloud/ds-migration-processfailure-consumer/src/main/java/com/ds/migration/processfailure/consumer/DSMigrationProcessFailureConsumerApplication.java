package com.ds.migration.processfailure.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationProcessFailureConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationProcessFailureConsumerApplication.class, args);
	}

}