package com.ds.migration.batchtrigger.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationBatchTriggerConsumerApplication {

	@Value("${migration.processstartqueue.threadpoolsize}")
	private Integer threadPoolSize;

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationBatchTriggerConsumerApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(threadPoolSize);
		threadPoolTaskScheduler.setThreadNamePrefix("MigrationThreadPoolTaskScheduler");
		return threadPoolTaskScheduler;
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

}