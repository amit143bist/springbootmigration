package com.ds.migration.authentication.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.authentication.controller.MigrationAuthenticationController;
import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-unittest.yml")
public class DSOauthServiceTest {

	@Autowired
	private MigrationAuthenticationController migrationAuthenticationController;

	@MockBean
	private RestTemplate restTemplate;

	@MockBean
	private HttpHeaders httpHeaders;

	@MockBean
	private DSOauthService dsOauthService;

	@Autowired
	CacheManager manager;

	@Test
	public void testTokenIscached() {
		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
		authenticationResponse.setAccessToken("token1");
		ResponseEntity<AuthenticationResponse> obj1 = new ResponseEntity<>(authenticationResponse, HttpStatus.OK);

		authenticationResponse = new AuthenticationResponse();
		authenticationResponse.setAccessToken("token2");
		ResponseEntity<AuthenticationResponse> obj2 = new ResponseEntity<>(authenticationResponse, HttpStatus.OK);

		Mockito.doReturn(obj1).when(dsOauthService).requestOauthToken(
				"/Users/pedro.barroso/dev/migration/ds-public.key", "/Users/pedro.barroso/dev/migration/ds-private.key",
				"account-d.docusign.com", "8d72617c-675f-40e2-8f2e-439e5d2e95b2", "user1", Long.valueOf(3600),
				"scopes");
		Mockito.doReturn(obj2).when(dsOauthService).requestOauthToken(
				"/Users/pedro.barroso/dev/migration/ds-public.key", "/Users/pedro.barroso/dev/migration/ds-private.key",
				"account-d.docusign.com", "8d72617c-675f-40e2-8f2e-439e5d2e95b2", "user2", Long.valueOf(3600),
				"scopes");
		AuthenticationRequest authenticationRequestUser1 = new AuthenticationRequest("user1", "scopes");
		AuthenticationRequest authenticationRequestUser2 = new AuthenticationRequest("user2", "scopes");

		// First invocation returns object returned by the method
		ResponseEntity<AuthenticationResponse> result = migrationAuthenticationController
				.requestJWTUserToken(authenticationRequestUser1);
		assertThat(result, is(obj1));

		// Second invocation should return cached value, *not* second (as set up above)
		result = migrationAuthenticationController.requestJWTUserToken(authenticationRequestUser1);
		assertThat(result, is(obj1));

		// Verify repository method was invoked once
		Mockito.verify(dsOauthService, Mockito.times(1)).requestOauthToken(
				"/Users/pedro.barroso/dev/migration/ds-public.key", "/Users/pedro.barroso/dev/migration/ds-private.key",
				"account-d.docusign.com", "8d72617c-675f-40e2-8f2e-439e5d2e95b2", "user1", Long.valueOf(3600),
				"scopes");
		assertThat(manager.getCache("token").get("user1"), is(notNullValue()));

		// Third invocation with different key is triggers the second invocation of the
		// repo method
		result = migrationAuthenticationController.requestJWTUserToken(authenticationRequestUser2);
		assertThat(result, is(obj2));

	}

	@Test
	public void testInvalidateCache() throws InterruptedException {
		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
		authenticationResponse.setAccessToken("token1");
		ResponseEntity<AuthenticationResponse> obj1 = new ResponseEntity<>(authenticationResponse, HttpStatus.OK);

		Mockito.doReturn(obj1).when(dsOauthService).requestOauthToken(
				"/Users/pedro.barroso/dev/migration/ds-public.key", "/Users/pedro.barroso/dev/migration/ds-private.key",
				"account-d.docusign.com", "8d72617c-675f-40e2-8f2e-439e5d2e95b2", "user1", Long.valueOf(4), "scopes");
		AuthenticationRequest authenticationRequestUser1 = new AuthenticationRequest("user1", "scopes");

		// First invocation returns object returned by the method
		migrationAuthenticationController.requestJWTUserToken(authenticationRequestUser1);
		assertThat(manager.getCache("token").get("user1"), is(notNullValue()));

		SECONDS.sleep(Long.valueOf(6));
		assertNull(manager.getCache("token").get("user1"));
	}
}