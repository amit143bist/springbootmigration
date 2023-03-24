package com.ds.migration.feign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.auth.BasicAuthRequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignClientConfiguration {

	@Value("${migration.application.username}")
	private String basicAuthUserName;

	@Value("${migration.application.password}")
	private String basicAuthUserPassword;

	@Bean
	public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
		log.info("basicAuthUserName is {}, basicAuthUserPassword is {}", basicAuthUserName, basicAuthUserPassword);
		return new BasicAuthRequestInterceptor(basicAuthUserName, basicAuthUserPassword);
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public FeignErrorDecoder errorDecoder() {
		return new FeignErrorDecoder(objectMapper());
	}
}