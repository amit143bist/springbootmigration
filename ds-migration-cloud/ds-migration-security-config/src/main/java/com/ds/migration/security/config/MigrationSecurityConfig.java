package com.ds.migration.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import lombok.extern.slf4j.Slf4j;

@Profile({ "!unittest" })
@Slf4j
public class MigrationSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${migration.application.username}")
	private String basicAuthUserName;

	@Value("${migration.application.password}")
	private String basicAuthUserPassword;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

		log.info("Configure(auth) method called with username {} and password {}", basicAuthUserName,
				basicAuthUserPassword);
		auth.inMemoryAuthentication().withUser(basicAuthUserName).password(basicAuthUserPassword).roles("USER");

	}

	// Secure the endpoints with HTTP Basic authentication
	@Override
	protected void configure(HttpSecurity http) throws Exception {

		log.info("configure(http) method called");
		http
				// HTTP Basic authentication
				.httpBasic().and().authorizeRequests().antMatchers(HttpMethod.GET, "/migration/**").hasRole("USER")
				.antMatchers(HttpMethod.POST, "/migration/**").hasRole("USER")
				.antMatchers(HttpMethod.PUT, "/migration/**").hasRole("USER")
				.antMatchers(HttpMethod.PATCH, "/migration/**").hasRole("USER")
				.antMatchers(HttpMethod.DELETE, "/migration/**").hasRole("USER").and().csrf().disable().formLogin()
				.disable();
	}
}