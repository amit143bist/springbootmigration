package com.ds.migration.processcomplete.consumer;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
@Profile({ "unittest" })
public class TestAuditConfiguration {

	@Bean
	public AuditorAware<String> auditorTestProvider() {
		return new TestAuditorAwareImpl();
	}

	@Bean
	public Pbkdf2PasswordEncoder pbkdf2PasswordEncoder() {
		return new Pbkdf2PasswordEncoder();
	}
}

class TestAuditorAwareImpl implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {

		return Optional.of("Mr Test Auditor");
	}
}