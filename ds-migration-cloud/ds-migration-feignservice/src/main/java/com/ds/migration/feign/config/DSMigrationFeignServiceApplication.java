package com.ds.migration.feign.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationFeignServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationFeignServiceApplication.class, args);
	}
}