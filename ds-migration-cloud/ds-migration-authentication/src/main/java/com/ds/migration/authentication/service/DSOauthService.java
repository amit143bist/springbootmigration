package com.ds.migration.authentication.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.authentication.utils.JWTUtils;
import com.ds.migration.common.exception.ConsentRequiredException;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DSOauthService {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private HttpHeaders headers;

	@Value("${ds.migration.token.testtoken}")
	private String testToken;

	public ResponseEntity<AuthenticationResponse> requestOauthToken(String rsaPublicKeyPath, String rsaPrivateKeyPath,
			String baseUrl, String integratorKey, String user, Long expiredInSeconds, String scopes) {
		// pass the path to the file as a parameter
		try {

			log.info("Generating Access Token");
			String assertion = JWTUtils.generateJWTAssertion(rsaPublicKeyPath, rsaPrivateKeyPath, baseUrl,
					integratorKey, user, expiredInSeconds, scopes);

			log.info("JWT assertion {} for user {} with IntegratorKey {}", assertion, user, integratorKey);

			Assert.notNull(assertion, "assertion was empty");

			MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
			form.add("assertion", assertion);
			form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");

			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.setCacheControl(CacheControl.noStore());
			headers.setPragma("no-cache");

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
			return Optional.ofNullable(

					restTemplate.exchange("https://" + baseUrl + "/oauth/token", HttpMethod.POST, request,
							AuthenticationResponse.class))
					.map(authenticationToken -> {

						Assert.notNull(authenticationToken.getBody(), "authenticationToken is null for user " + user);
						Assert.isTrue(authenticationToken.getStatusCode().is2xxSuccessful(),
								"AuthenticationToken is not returned with 200 status code");

						log.info("Returning Access Token -> {} for user {} with IntegratorKey {}", authenticationToken,
								user, integratorKey);

						if (!StringUtils.isEmpty(testToken)) {

							AuthenticationResponse authenticationResponse = new AuthenticationResponse();
							authenticationResponse.setAccessToken(testToken);
							authenticationResponse.setTokenType("Bearer");
							authenticationResponse.setExpiresIn(3600);

							return new ResponseEntity<AuthenticationResponse>(authenticationResponse,
									HttpStatus.CREATED);
						}

						return new ResponseEntity<AuthenticationResponse>(authenticationToken.getBody(),
								HttpStatus.CREATED);
					}).orElseThrow(
							() -> new ResourceNotFoundException("Token was not retrieved from authenticationToken"));
		} catch (HttpClientErrorException e) {

			log.error(
					"HttpClientErrorException {} happened in generating Access Token for user {} with IntegratorKey {}",
					e.getMessage(), user, integratorKey);

			log.error("HttpClientErrorException statusCode is {} and ResponseBody is  {}", e.getStatusCode(),
					e.getResponseBodyAsString());

			if (e.getStatusCode() == HttpStatus.BAD_REQUEST
					&& e.getResponseBodyAsString().contains("consent_required")) {

				String consentUrl = "https://" + baseUrl + "/oauth/auth?response_type=code&scope=" + scopes
						+ "&client_id=" + integratorKey + "redirect_uri=https://www.docusign.com";
				throw new ConsentRequiredException(
						"Unable to Obtain token for user: " + user + ". " + "Error description: "
								+ e.getResponseBodyAsString() + " Obtain token by launching " + consentUrl);
			}

			throw e;
		} catch (IOException e) {

			log.error("IOException {} happened in generating Access Token for user {} with IntegratorKey {}",
					e.getMessage(), user, integratorKey);

			throw new ConsentRequiredException("Unable to read key " + e.getMessage());
		}
	}
}
