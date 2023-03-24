package com.ds.migration.security.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationSecurityConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationSecurityConfigApplication.class, args);
	}

}