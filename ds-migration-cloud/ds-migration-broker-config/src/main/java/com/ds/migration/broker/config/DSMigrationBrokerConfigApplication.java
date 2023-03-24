package com.ds.migration.broker.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationBrokerConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationBrokerConfigApplication.class, args);
	}

}