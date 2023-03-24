package com.ds.migration.prontodata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.security.config.MigrationSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
@EnableDiscoveryClient
@EnableWebSecurity
@EnableSwagger2
public class DSMigrationProntoDataApplication {

	@Value("${migration.application.async.executornameprefix}")
	private String executorNamePrefix;
	@Value("${migration.application.async.corepoolsize}")
	private int corePoolSize;
	@Value("${migration.application.async.maxpoolsize}")
	private int maxPoolSize;
	@Value("${migration.application.async.queuecapacity}")
	private int queueCapacity;

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationProntoDataApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public HttpHeaders httpHeaders() {
		return new HttpHeaders();
	}

	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new MigrationSecurityConfig();
	}

	@Bean
	public Docket api() {

		return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any()).build();

	}

	@Bean(name = "ProntoTaskExecutor")
	public TaskExecutor prontoTaskExecutor() {

		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(executorNamePrefix);
		return executor;
	}

}