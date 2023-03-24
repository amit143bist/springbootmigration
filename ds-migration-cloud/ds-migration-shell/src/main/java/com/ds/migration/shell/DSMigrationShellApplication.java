package com.ds.migration.shell;

import java.io.IOException;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = { "com.ds.migration" })
public class DSMigrationShellApplication {

	public static void main(String[] args) throws IOException {

		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(DSMigrationShellApplication.class)
				.bannerMode(Banner.Mode.CONSOLE).web(WebApplicationType.NONE).build().run(args);

		SpringApplication.exit(ctx, () -> 0);
	}

}