package com.ds.migration.authentication.controller;

import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
public class MigrationAuthenticationControllerTest extends AbstractTests {

	@Test
	public void requestJWTUserTokenUserWithoutConsent() throws Exception {
		mockMvc.perform(
				MockMvcRequestBuilders.post("/migration/authentication/token")
						.with(httpBasic("migrationuser", "mIgratIonpassword1"))
						.content(asJsonString(new AuthenticationRequest("90863ea8-14fc-441e-a48c-17bd78dc6bef", "signature impersonation")))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.message").isString());

	}

	@Test
	public void requestJWTUserTokenUserWithConsent() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/migration/authentication/token")
				.with(httpBasic("migrationuser", "mIgratIonpassword1"))
				.content(asJsonString(new AuthenticationRequest("21dbfafb-8bc8-4a96-8f22-4ac603d66479", "signature impersonation")))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.access_token").isNotEmpty()).andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").value(3600));

	}
}