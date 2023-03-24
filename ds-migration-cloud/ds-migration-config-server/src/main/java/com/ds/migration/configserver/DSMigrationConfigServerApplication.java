package com.ds.migration.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
@EnableConfigServer
public class DSMigrationConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationConfigServerApplication.class, args);
	}

}
