package com.ds.migration.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationWebsiteApplication {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public HttpHeaders httpHeaders() {
		return new HttpHeaders();
	}

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationWebsiteApplication.class, args);
	}

}