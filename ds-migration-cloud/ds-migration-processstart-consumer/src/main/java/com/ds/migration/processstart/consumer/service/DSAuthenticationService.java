package com.ds.migration.processstart.consumer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.common.exception.AuthenticationTokenException;
import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;
import com.ds.migration.processstart.consumer.client.MigrationAuthenticationClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Service
@Slf4j
public class DSAuthenticationService {

	@Value("${migration.dsmigrationauthentication.userid}")
	private String dsUserId;

	@Value("${migration.dsmigrationauthentication.scopes}")
	private String dsScopes;

	@Autowired
	private MigrationAuthenticationClient migrationAuthenticationClient;

	private AuthenticationResponse authenticationResponse;

	public void fetchDSAuthToken(String processId) {

		log.debug("AuthenticationResponse is called for processId -> {}", processId);

		if (null == processId) {

			log.info("Accesstoken created from exception block of invokeAPI or null check");
		}

		log.debug("In fetchDSAuthToken dsUserId {}, scopes {} ", dsUserId, dsScopes);

		try {

			AuthenticationResponse authenticationResponse = migrationAuthenticationClient
					.requestJWTUserToken(new AuthenticationRequest(dsUserId, dsScopes)).getBody();
			setAuthenticationResponse(authenticationResponse);
		} catch (Exception exp) {

			log.error("AuthenticationTokenException {} with message {} occurred for processId -> {}", exp.getCause(),
					exp.getMessage(), processId);
			throw new AuthenticationTokenException(exp.getMessage() + "_" + exp.getLocalizedMessage(), exp.getCause());
		}
	}
}