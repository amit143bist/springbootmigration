package com.ds.migration.website.service;

import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;
import com.ds.migration.feign.website.domain.AttachmentResponse;
import com.ds.migration.feign.website.domain.SignatureInformationResponse;
import com.ds.migration.website.client.MigrationAuthenticationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DocusignService {

    @Autowired
    MigrationAuthenticationClient migrationAuthenticationClient;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${docusign.api.baserUrl}")
    private java.lang.String baserUrl;

    @Value("${docusign.api.account}")
    private java.lang.String accountId;

    @Value("${docusign.api.scopes}")
    private java.lang.String scopes;

    @Value("${docusign.api.user}")
    private java.lang.String user;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpHeaders headers;

    private HttpEntity<String> prepareHTTPEntity(String token) {

        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer" + " " + token);

        return new HttpEntity<>(headers);
    }

    public List<SignatureInformationResponse> getSignaturesInformation(Long documentId, Long signatureId,
                                                                       String envelopeId) {
        Assert.notNull(envelopeId, "envelopeId is empty");
        Assert.notNull(signatureId, "signatureId is empty");
        Assert.notNull(documentId, "documentId is empty");
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(user, scopes);
        AuthenticationResponse response = migrationAuthenticationClient.requestJWTUserToken(authenticationRequest)
                .getBody();

        Assert.notNull(response, "AuthenticationResponse is empty");
		AttachmentResponse attachmentResponse = getAttachments(envelopeId, documentId,
                prepareHTTPEntity(response.getAccessToken()));

        Assert.notEmpty(attachmentResponse.getAttachments(), "Attachment is empty");

        return getSignatures(envelopeId, attachmentResponse.getAttachments().get(0).getAttachmentId(), signatureId,
                prepareHTTPEntity(response.getAccessToken()));
    }

	private AttachmentResponse getAttachments(String envelopeId, Long document, HttpEntity<String> entity) {
        try{
            log.info("DocusignService.getAttachments envelopeId " + envelopeId);

            return Optional.ofNullable(
                    restTemplate.exchange(baserUrl + "/v2.1/accounts/{accountId}/envelopes/{envelopeId}/attachments",
                            HttpMethod.GET, entity, AttachmentResponse.class, accountId, envelopeId))
                    .map(attachments -> {

                        Assert.notNull(attachments.getBody().getAttachments(),
							"Attachments is null for enevelopeId " + envelopeId);
                        Assert.isTrue(attachments.getStatusCode().is2xxSuccessful(),
                                " attachments response was did not return 200 status code");

                        return attachments.getBody();
                    }).orElseThrow(() -> new ResourceNotFoundException(
                            "Attachments entries not found for envelopeId " + envelopeId));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Invalid envelopeId {}  assigned to documentId {}", envelopeId, document);
                throw new ResourceNotFoundException(
                        "Invalid envelope assigned to documentId " +  document);
            }
            throw e;
        }
    }

    private List<SignatureInformationResponse> getSignatures(String envelopeId, String attachmentId, Long signatureId,
                                                             HttpEntity<String> entity) {

        log.info("DocusignService.getSignatures envelopeId " + envelopeId);

        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter
                .setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);

        return Optional.ofNullable(restTemplate.exchange(
                baserUrl + "/v2.1/accounts/{accountId}/envelopes/{envelopeId}/attachments/{attachmentId}",
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<SignatureInformationResponse>>() {
                }, accountId, envelopeId, attachmentId)).map(signatures -> {

            Assert.notNull(signatures.getBody(), "Signatures is null for envelopeId " + envelopeId);
            Assert.isTrue(signatures.getStatusCode().is2xxSuccessful(),
                    "ProntoSignedDrafts is not returned with 200 status code");

            // Now let's select the default
            for (SignatureInformationResponse signatureInformationResponse : signatures.getBody()) {

                signatureInformationResponse.set_default("0");
                if (Long.valueOf(signatureInformationResponse.getSignatureid()).equals(signatureId)) {
                    signatureInformationResponse.set_default("1");
                }
            }
            return signatures.getBody();
        }).orElseThrow(() -> new ResourceNotFoundException(
                "Attachments entries not found for envelopeId " + envelopeId));
    }

    public byte[] getDocument(String envelopeId, Long documentId) {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(user, scopes);
        AuthenticationResponse response = migrationAuthenticationClient.requestJWTUserToken(authenticationRequest)
                .getBody();

        return Optional.ofNullable(restTemplate.exchange(
                baserUrl + "/v2.1/accounts/{accountId}/envelopes/{envelopeId}/documents/{documentId}", HttpMethod.GET,
                prepareHTTPEntity(response.getAccessToken()), byte[].class, accountId, envelopeId, documentId))
                .map(document -> {

                    Assert.notNull(document.getBody(),
                            "documentId " + documentId + " was null for envelopeId " + envelopeId);
                    Assert.isTrue(document.getStatusCode().is2xxSuccessful(),
                            "document is not returned with 200 status code");

                    return document.getBody();
                }).orElseThrow(() -> new ResourceNotFoundException(
                        "Attachments entries not found for envelopeId " + envelopeId));
    }
}
