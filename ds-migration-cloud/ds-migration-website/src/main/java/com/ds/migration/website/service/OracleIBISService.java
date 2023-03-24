package com.ds.migration.website.service;

import java.nio.charset.Charset;
import java.util.Optional;

import com.ds.migration.common.exception.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.feign.website.domain.EnvelopeMappingResponse;
import com.ds.migration.feign.website.domain.IbisTokenRequest;
import com.ds.migration.feign.website.domain.IbisTokenResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OracleIBISService {

	@Value("${oracle.ibis.jwt.url}")
	private String ibisJwtUrl;

	@Value("${oracle.ibis.jwt.audience}")
	private String ibisJwtAudience;

	@Value("${oracle.ibis.jwt.user}")
	private String ibisJwtUser;

	@Value("${oracle.ibis.jwt.password}")
	private String ibisJwtPass;

	@Value("${oracle.ibis.api.url}")
	private String ibisApiUrl;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private HttpHeaders headers;

	public String getEnvelopeByDocument(Long documentId, String accesstoken) {

		log.debug("Retrieving envelope Id assigned to document " + documentId);
		Assert.notNull(documentId, "documentId was empty");

		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer" + " " + getIbisToken());
		HttpEntity<String> entity = new HttpEntity<>(headers);
		log.debug ("Calling OracleIBISService.getEnvelopeByDocument for resource {}",
				ibisApiUrl + "/documents/prontodoc/documentid/{}/accesstoken/{}", documentId, accesstoken);
		try {
            return Optional
                    .ofNullable(restTemplate.exchange(ibisApiUrl + "/documents/prontodoc/documentid/{documentId}/accesstoken/{accesstoken}",
                            HttpMethod.GET, entity, EnvelopeMappingResponse.class, documentId, accesstoken))
                    .map(tpmDocumentId -> {

                        Assert.isTrue(tpmDocumentId.getStatusCode().is2xxSuccessful(),
                                "Docusign envelope is not returned with 200 status code for documentId# " + documentId);
                        Assert.notNull(tpmDocumentId.getBody(), "EnvelopeMappingResponse is null");

                        if (null == tpmDocumentId.getBody().getEnvelopeid() ||
                                tpmDocumentId.getBody().getEnvelopeid().isEmpty()){
                           throw new EnvelopeNotCreatedException("DocumentID "+ documentId + " has not been migrated yet.");
                        } else {
                            return tpmDocumentId.getBody().getEnvelopeid();
                        }

                    }).orElseThrow(
                            () -> new ResourceNotFoundException("TPM data exception thrown for documentId# " + documentId));
        } catch(HttpClientErrorException e){

			log.info("Calling OracleIBISService.getEnvelopeByDocument: Receive HttpClientErrorException {}", e.getStatusCode());
			throw new DocumentNotFoundException("Unable to retrieve document from IBIS", e);

        }
	}

	private String getIbisToken() {

		Assert.notNull(ibisJwtUrl, "Ibis JWT host was empty");
		Assert.notNull(ibisJwtUser, "Ibis JWT user was empty");
		Assert.notNull(ibisJwtPass, "Ibis JWT password was null");

		if (ibisJwtAudience == null) {
			ibisJwtAudience = "ibis";
		}
		log.info("Getting token for user {}, Audience = {},  ibisJwtUrl = {}", ibisJwtUser, ibisJwtAudience, ibisJwtUrl);

		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		addBasicAuth(ibisJwtUser, ibisJwtPass);
		IbisTokenRequest ibisTokenRequest = new IbisTokenRequest(ibisJwtAudience, "d", 1);

		HttpEntity<IbisTokenRequest> request = new HttpEntity<>(ibisTokenRequest, headers);

		try {
			return Optional.ofNullable(

					restTemplate.exchange(ibisJwtUrl + "/keys", HttpMethod.POST, request,
							IbisTokenResponse.class))
					.map(ibisToken -> {

						Assert.notNull(ibisToken.getBody(), "ibisToken is null for user " + ibisJwtUser);
						Assert.isTrue(ibisToken.getStatusCode().is2xxSuccessful(),
								"AuthenticationToken is not returned with 200 status code");

						return ibisToken.getBody().getKey();
					}).orElseThrow(
							() -> new ResourceNotFoundException("Token was not retrieve from authenticationToken"));

		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {

				throw new ConsentRequiredException("Unable to Obtain token for user: " + ibisJwtUser + ". "
						+ "Error description: " + e.getResponseBodyAsString());
			}
			throw e;
		}
	}

	private void addBasicAuth(String username, String password) {
		log.info("Adding basic Auth for user {} ", username);
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
		headers.set("Authorization", "Basic " + new String(encodedAuth));
	}

}
