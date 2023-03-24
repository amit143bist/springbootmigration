package com.ds.migration.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationCommonApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationCommonApplication.class, args);
	}

}