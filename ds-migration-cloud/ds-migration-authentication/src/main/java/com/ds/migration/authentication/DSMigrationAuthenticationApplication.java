package com.ds.migration.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.security.config.MigrationSecurityConfig;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
@EnableDiscoveryClient // This will register this service with Eureka
@EnableCaching
@EnableWebSecurity
@EnableScheduling // This enables scheduling to clear the cache
@EnableSwagger2
public class DSMigrationAuthenticationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationAuthenticationApplication.class, args);
	}

	@Value("${ds.migration.token.cacheExpirationSeconds}")
	private java.lang.String cacheExpirationSeconds;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public HttpHeaders httpHeaders() {
		return new HttpHeaders();
	}

	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("token");
	}

	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new MigrationSecurityConfig();
	}

	@Bean
	public String getScheduleFixedRate() {
		return Long.toString(Long.valueOf(cacheExpirationSeconds) * 1000);
	}

	@Bean
	public Docket api() {

		return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any()).build();

	}
}