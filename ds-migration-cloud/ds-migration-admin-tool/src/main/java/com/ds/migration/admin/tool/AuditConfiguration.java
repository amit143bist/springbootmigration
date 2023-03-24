package com.ds.migration.admin.tool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
@Profile({ "!unittest" })
public class AuditConfiguration {

	@Bean
	public Pbkdf2PasswordEncoder pbkdf2PasswordEncoder() {
		return new Pbkdf2PasswordEncoder();
	}

}