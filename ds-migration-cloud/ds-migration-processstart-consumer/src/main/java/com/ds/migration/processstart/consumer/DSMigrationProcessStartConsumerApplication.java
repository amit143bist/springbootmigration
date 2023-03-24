package com.ds.migration.processstart.consumer;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationProcessStartConsumerApplication {

	@Value("${migration.application.async.executornameprefix}")
	private String executorNamePrefix;
	@Value("${migration.application.async.corepoolsize}")
	private int corePoolSize;
	@Value("${migration.application.async.maxpoolsize}")
	private int maxPoolSize;
	@Value("${migration.application.async.queuecapacity}")
	private int queueCapacity;

	@Value("${migration.application.connection.ds.proxyhost}")
	private String dsProxyHost;
	@Value("${migration.application.connection.ds.proxyport}")
	private String dsProxyPort;

	@Value("${migration.application.connection.pronto.proxyhost}")
	private String prontoProxyHost;
	@Value("${migration.application.connection.pronto.proxyport}")
	private String prontoProxyPort;

	public static void main(String[] args) {
		SpringApplication.run(DSMigrationProcessStartConsumerApplication.class, args);
	}

	@Bean(name = "dsRestTemplate")
	public RestTemplate dsRestTemplate() {

		return createRestTemplate(dsProxyHost, dsProxyPort);
	}

	@Bean(name = "prontoRestTemplate")
	public RestTemplate prontoRestTemplate() {

		return createRestTemplate(prontoProxyHost, prontoProxyPort);
	}

	private RestTemplate createRestTemplate(String proxyHost, String proxyPort) {

		RestTemplate restTemplate = null;

		if (!StringUtils.isEmpty(proxyHost) && !StringUtils.isEmpty(proxyPort)) {

			SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
			clientHttpRequestFactory.setProxy(proxy);

			restTemplate = new RestTemplate(clientHttpRequestFactory);
		} else {
			restTemplate = new RestTemplate();
		}
		return restTemplate;
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public HttpHeaders httpHeaders() {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

		return httpHeaders;
	}

	@Bean(name = "RecordTaskExecutor")
	public TaskExecutor recordTaskExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(executorNamePrefix);
		return executor;
	}
}