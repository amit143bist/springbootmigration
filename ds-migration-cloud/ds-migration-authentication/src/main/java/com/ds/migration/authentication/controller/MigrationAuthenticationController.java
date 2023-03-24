package com.ds.migration.authentication.controller;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.migration.authentication.service.DSOauthService;
import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;
import com.ds.migration.feign.authentication.service.MigrationAuthenticationService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class MigrationAuthenticationController implements MigrationAuthenticationService {

	@Value("${ds.migration.api.integratorKey}")
	private String integratorKey;

	@Value("${ds.migration.token.expirationSeconds}")
	private String expirationSeconds;

	@Value("${ds.authorization.rsaPrivateKeyPath}")
	private String rsaPrivateKeyPath;

	@Value("${ds.authorization.rsaPublicKeyPath}")
	private String rsaPublicKeyPath;

	@Value("${migration.application.dsenvironment}")
	private String dsEnvironment;

	@Autowired
	private DSOauthService dsOauthService;

	@Autowired
	private CacheManager cacheManager;

	/**
	 * Generates a JWT assertion and sign it then use that assertion to obtain a
	 * oauth token.
	 *
	 * @param authenticationRequest This is the object with the user and teh scopes
	 *                              for the assertion
	 * @return a {@link AuthenticationResponse} containing the oAuth2 token
	 */
	@Override
	@Cacheable(value = "token", key = "#authenticationRequest.user")
	public ResponseEntity<AuthenticationResponse> requestJWTUserToken(AuthenticationRequest authenticationRequest) {

		Assert.notNull(authenticationRequest.getUser(), "authenticationRequest.user was empty");
		Assert.notNull(authenticationRequest.getScopes(), "authenticationRequest.scopes was empty");

		log.debug("MigrationAuthenticationController.requestJWTUserToken() user -> {} scopes -> {}",
				authenticationRequest.getUser(), authenticationRequest.getScopes());

		log.info("Calling dsOauthService.requestOauthToken rsaPublicKeyPath -> {}, rsaPrivateKeyPath -> {},"
				+ "dsEnvironment -> {}, integratorKey -> {}, user -> {}, expirationSeconds -> {}, scopyes -> {}",
				rsaPublicKeyPath, rsaPrivateKeyPath, dsEnvironment, integratorKey, authenticationRequest.getUser(),
				Long.valueOf(expirationSeconds), authenticationRequest.getScopes());

		return dsOauthService.requestOauthToken(rsaPublicKeyPath, rsaPrivateKeyPath, dsEnvironment, integratorKey,
				authenticationRequest.getUser(), Long.valueOf(expirationSeconds), authenticationRequest.getScopes());

	}

	@PutMapping(value = "/migration/authentication/token/evictcache")
	public String clearCache() {

		log.info("clearCache scheduled called, now clearing the tokens for all users");
		cacheManager.getCache("token").clear();

		return ValidationResult.SUCCESS.toString();
	}

	/**
	 * Clear all tokens from token cache, every
	 */
	@Scheduled(fixedRateString = "#{@getScheduleFixedRate}")
	public void evictAuthenticationCache() {
		log.info("evictAuthenticationCache scheduled called, now clearing the tokens for all users");
		cacheManager.getCache("token").clear();
	}
}
