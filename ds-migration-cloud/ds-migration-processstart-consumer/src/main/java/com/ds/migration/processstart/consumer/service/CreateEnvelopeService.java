package com.ds.migration.processstart.consumer.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.constant.RecordProcessPhaseStatus;
import com.ds.migration.common.exception.DocumentNotFoundException;
import com.ds.migration.common.exception.DocumentValidationFailureException;
import com.ds.migration.common.exception.EnvelopeNotCreatedException;
import com.ds.migration.common.exception.JSONConversionException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;
import com.ds.migration.feign.process.domain.Attachment;
import com.ds.migration.feign.process.domain.AttachmentDefinition;
import com.ds.migration.feign.process.domain.CarbonCopy;
import com.ds.migration.feign.process.domain.CompositeTemplate;
import com.ds.migration.feign.process.domain.Document;
import com.ds.migration.feign.process.domain.EnvelopeArchiveDefinition;
import com.ds.migration.feign.process.domain.EnvelopeDefinition;
import com.ds.migration.feign.process.domain.EnvelopeResponse;
import com.ds.migration.feign.process.domain.InlineTemplate;
import com.ds.migration.feign.process.domain.Recipients;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CreateEnvelopeService {

	@Value("${migration.docusign.ccname}")
	private String ccName;

	@Value("${migration.docusign.ccemail}")
	private String ccEmail;

	@Value("${migration.docusign.emailsubject}")
	private String emailSubject;

	@Value("${migration.dsmigrationauthentication.userid}")
	private String dsUserId;

	@Value("${migration.dsmigrationauthentication.scopes}")
	private String dsScopes;

	@Value("${migration.dsmigrationauthentication.legacyauthentication}")
	private String legacyAuthentication;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	@Qualifier("dsRestTemplate")
	private RestTemplate dsRestTemplate;

	@Autowired
	private HttpHeaders httpHeaders;

	@Autowired
	private RetrieveProntoDocumentService retrieveProntoDocumentService;

	@Autowired
	private DSAuthenticationService dsAuthenticationService;

	public String createEnvelope(List<MigrationProntoDataResponse> migrationSignedDraftList, String dsURL,
			String processId, String recordId, boolean envelopeArchiveAvailable,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		log.info("CreateEnvelope is called for processId -> {}, recordId -> {} and envelopeArchiveAvailable is {}",
				processId, recordId, envelopeArchiveAvailable);

		MigrationProntoDataResponse migrationProntoDataResponse = migrationSignedDraftList
				.get(migrationSignedDraftList.size() - 1);// highest documentId

		if (envelopeArchiveAvailable) {

			log.debug("Calling EnvelopeArchive Service for recordId -> {} and processId -> {}", recordId, processId);

			return invokeEnvelopeArchiveDefinition(dsURL, migrationSignedDraftList, processId, recordId,
					migrationProntoDataResponse, migrationAuditDataRequestList);
		} else {

			log.debug("Calling Envelope Service for recordId -> {} and processId -> {}", recordId, processId);
			return invokeEnvelopeDefinition(dsURL, migrationSignedDraftList, processId, recordId,
					migrationProntoDataResponse, migrationAuditDataRequestList);
		}

	}

	private String invokeEnvelopeDefinition(String dsURL, List<MigrationProntoDataResponse> migrationSignedDraftList,
			String processId, String recordId, MigrationProntoDataResponse migrationProntoDataResponse,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		EnvelopeDefinition envelopeDefinition = createEnvelopeDefinition(migrationSignedDraftList, processId, recordId,
				migrationProntoDataResponse, migrationAuditDataRequestList);

		String envelopeId = invokeAPI(EnvelopeResponse.class, dsURL, envelopeDefinition, recordId, HttpMethod.POST)
				.getEnvelopeId();

		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(createAttachment(recordId, migrationSignedDraftList, processId));

		AttachmentDefinition attachmentDefinition = new AttachmentDefinition();
		attachmentDefinition.setAttachments(attachments);

		invokeAPI(AttachmentDefinition.class,
				dsURL + AntPathMatcher.DEFAULT_PATH_SEPARATOR + envelopeId + MigrationAppConstants.ATTACHMENT_PATH,
				attachmentDefinition, recordId, HttpMethod.PUT);

		envelopeDefinition = new EnvelopeDefinition();
		envelopeDefinition.setStatus(MigrationAppConstants.ENVELOPE_SENT_STATUS);

		return invokeAPI(EnvelopeResponse.class, dsURL + AntPathMatcher.DEFAULT_PATH_SEPARATOR + envelopeId,
				envelopeDefinition, recordId, HttpMethod.PUT).getEnvelopeId();

	}

	private EnvelopeDefinition createEnvelopeDefinition(List<MigrationProntoDataResponse> migrationSignedDraftList,
			String processId, String recordId, MigrationProntoDataResponse migrationProntoDataResponse,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
		envelopeDefinition.setStatus(MigrationAppConstants.ENVELOPE_CREATED_STATUS);
		envelopeDefinition.setEmailSubject(emailSubject);
		envelopeDefinition.setCompositeTemplates(createCompositeTemplates(processId, recordId,
				migrationProntoDataResponse, migrationAuditDataRequestList));
		return envelopeDefinition;
	}

	private List<CompositeTemplate> createCompositeTemplates(String processId, String recordId,
			MigrationProntoDataResponse migrationProntoDataResponse,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		CompositeTemplate compositeTemplate = new CompositeTemplate();
		compositeTemplate.setDocument(
				createDocument(migrationProntoDataResponse, processId, recordId, migrationAuditDataRequestList));
		compositeTemplate.setInlineTemplates(createInlineTemplates());

		List<CompositeTemplate> compositeTemplates = new ArrayList<CompositeTemplate>();
		compositeTemplates.add(compositeTemplate);
		return compositeTemplates;
	}

	private List<InlineTemplate> createInlineTemplates() {

		List<CarbonCopy> carbonCopies = new ArrayList<CarbonCopy>();
		carbonCopies.add(createCarbonCopy());

		Recipients recipients = new Recipients();
		recipients.setCarbonCopies(carbonCopies);

		InlineTemplate inlineTemplate = new InlineTemplate();
		inlineTemplate.setSequence(CustomBooleanEditor.VALUE_1);
		inlineTemplate.setRecipients(recipients);

		List<InlineTemplate> inlineTemplates = new ArrayList<InlineTemplate>();
		inlineTemplates.add(inlineTemplate);

		return inlineTemplates;
	}

	private <T> T invokeAPI(Class<T> returnType, String dsURL, Object msgBodyAsObject, String recordId,
			HttpMethod httpMethod) {

		String msgBody = null;
		ResponseEntity<T> callResp = null;
		HttpEntity<String> requestEntity = null;
		try {
			HttpHeaders httpHeadersLocal = getHeaders(recordId);

			msgBody = objectMapper.writeValueAsString(msgBodyAsObject);
			requestEntity = new HttpEntity<String>(msgBody, httpHeadersLocal);

			if (!MediaType.APPLICATION_JSON.equals(httpHeadersLocal.getContentType())) {

				log.error("Wrong Content Type {} is set for recordId -> {}, requestEntity is {}",
						httpHeadersLocal.getContentType(), recordId, requestEntity);
			}

			log.debug("DS API service is called for recordId -> {} with requestEntity {} and dsURL is {}", recordId,
					requestEntity, dsURL);

			callResp = dsRestTemplate.exchange(dsURL, httpMethod, requestEntity, returnType);

		} catch (HttpClientErrorException exp) {

			log.error(
					"Status Code returned is {}, rawStatusCode returned is {}, responseHeaders() is {}, responseBody is {} and msgBody is {} for recordId -> {}",
					exp.getStatusCode(), exp.getRawStatusCode(), exp.getResponseHeaders(),
					exp.getResponseBodyAsString(), msgBody, recordId);

			if (exp.getRawStatusCode() == 401) {

				log.info("AccessToken expired so generating new token and retrying this call for recordId -> {}",
						recordId);
				dsAuthenticationService.fetchDSAuthToken(null);

				return invokeAPI(returnType, dsURL, msgBodyAsObject, recordId, httpMethod);
			} else {

				throw new EnvelopeNotCreatedException("Envelope not created for recordId# " + recordId, exp.getCause());
			}

		} catch (JsonProcessingException exp) {

			log.error("JsonProcessingException occurred, exp message is {} for recordId -> {}", exp.getMessage(),
					recordId);
			throw new EnvelopeNotCreatedException("Envelope not created for recordId# " + recordId, exp.getCause());
		} catch (Exception exp) {

			log.error("Exception occurred, exp message is {} for recordId -> {}", exp.getMessage(), recordId);
			exp.printStackTrace();
			throw new EnvelopeNotCreatedException("Envelope not created for recordId# " + recordId, exp.getCause());
		}

		return callResp.getBody();
	}

	private HttpHeaders getHeaders(String recordId) {

		log.debug("AuthenticationResponse is called for recordId -> {}", recordId);

		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		AuthenticationResponse authenticationResponse = dsAuthenticationService.getAuthenticationResponse();

		if (null == authenticationResponse) {

			log.error("AuthenticationResponse is null in getHeaders for recordId -> {}", recordId);
			dsAuthenticationService.fetchDSAuthToken(null);
			authenticationResponse = dsAuthenticationService.getAuthenticationResponse();
		}

		if (!StringUtils.isEmpty(legacyAuthentication) && legacyAuthentication.contains("DocuSignCredentials")) {

			log.info("Legacy Auth Header is used for recordId -> {}", recordId);
			log.debug("Legacy Auth Header value is {}", legacyAuthentication);
			httpHeaders.set("X-DocuSign-Authentication", legacyAuthentication);
		} else {

			log.debug("OAuth Token is used {} for recordId -> {}", authenticationResponse.getAccessToken(), recordId);
			httpHeaders.set(HttpHeaders.AUTHORIZATION,
					authenticationResponse.getTokenType() + " " + authenticationResponse.getAccessToken());
		}

		return httpHeaders;
	}

	private String invokeEnvelopeArchiveDefinition(String dsURL,
			List<MigrationProntoDataResponse> migrationSignedDraftList, String processId, String recordId,
			MigrationProntoDataResponse migrationProntoDataResponse,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		log.debug("InvokeEnvelopeArchiveDefinition called for recordId -> {} and processId -> {}", recordId, processId);

		List<Document> documents = new ArrayList<Document>();
		documents.add(createDocument(migrationProntoDataResponse, processId, recordId, migrationAuditDataRequestList));

		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(createAttachment(recordId, migrationSignedDraftList, processId));

		List<CarbonCopy> carbonCopies = new ArrayList<CarbonCopy>();
		carbonCopies.add(createCarbonCopy());

		Recipients recipients = new Recipients();
		recipients.setCarbonCopies(carbonCopies);

		EnvelopeArchiveDefinition envelopeArchiveDefinition = new EnvelopeArchiveDefinition();
		envelopeArchiveDefinition.setStatus(MigrationAppConstants.ENVELOPE_SENT_STATUS);
		envelopeArchiveDefinition.setAttachments(attachments);
		envelopeArchiveDefinition.setDocuments(documents);
		envelopeArchiveDefinition.setRecipients(recipients);
		envelopeArchiveDefinition.setEmailSubject(emailSubject);

		return invokeAPI(EnvelopeResponse.class, dsURL, envelopeArchiveDefinition, recordId, HttpMethod.POST)
				.getEnvelopeId();
	}

	private Document createDocument(MigrationProntoDataResponse migrationProntoDataResponse, String processId,
			String recordId, List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		log.debug("CreateDocument is called for processId -> {}, recordId -> {}", processId, recordId);

		Document document = new Document();
		document.setDisplay(MigrationAppConstants.DOCUMENT_INLINE_DISPLAY);
		document.setDocumentId(CustomBooleanEditor.VALUE_1);
		document.setFileExtension(MigrationAppConstants.DOCUMENT_FILE_EXTENSION);
		document.setName(migrationProntoDataResponse.getDocumentid() + MigrationAppConstants.DOCUMENT_FILE_NAME_SUFFIX);
		document.setOrder(1);
		document.setDocumentBase64(Base64.getEncoder().encodeToString(retrieveProntoDocument(
				migrationProntoDataResponse, processId, recordId, migrationAuditDataRequestList)));

		log.debug("Document retrieved successfully from Pronto for recordId -> {} and processId -> {}", recordId,
				processId);
		migrationAuditDataRequestList.add(createAuditRequest(processId, recordId,
				RecordProcessPhase.FETCH_PRONTO_DOC.toString(), RecordProcessPhaseStatus.S.toString()));

		return document;
	}

	private byte[] retrieveProntoDocument(MigrationProntoDataResponse migrationProntoDataResponse, String processId,
			String recordId, List<MigrationAuditDataRequest> migrationAuditDataRequestList) {

		log.debug("VerifyDocument is called for processId -> {}, recordId -> {}", processId, recordId);

		String prontoUrl = retrieveProntoDocumentService.verifyDocument(migrationProntoDataResponse.getDocumentid(),
				migrationProntoDataResponse.getSignatureid(), migrationProntoDataResponse.getAccesstoken(), recordId,
				processId);

		log.debug("Document verified and prontoUrl in retrieveProntoDocument is {}", prontoUrl);
		if (null != prontoUrl) {

			if (MigrationAppConstants.DOCUMENT_FAILURE_STATUS.equalsIgnoreCase(prontoUrl)) {

				log.error("Document for recordId -> {} in processId {} failed validation using prontoUrl -> {}",
						recordId, processId, prontoUrl);

				throw new DocumentValidationFailureException("Document for recordId" + recordId + " in processId "
						+ processId + "failed validation using " + prontoUrl);
			} else {

				migrationAuditDataRequestList.add(createAuditRequest(processId, recordId,
						RecordProcessPhase.PRONTO_DOC_VERIFY.toString(), RecordProcessPhaseStatus.S.toString()));
				return retrieveProntoDocumentService.retrieveDocument(prontoUrl);
			}

		} else {

			log.error("Document not found for recordId -> {} in processId -> {}", recordId, processId);
			throw new DocumentNotFoundException("Unable to find document for recordId" + recordId + " in processId "
					+ processId + " using " + prontoUrl);
		}

	}

	private MigrationAuditDataRequest createAuditRequest(String processId, String recordId, String recordPhase,
			String recordPhaseStatus) {

		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest();
		migrationAuditDataRequest.setAuditEntryDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		migrationAuditDataRequest.setProcessId(processId);
		migrationAuditDataRequest.setRecordId(recordId);
		migrationAuditDataRequest.setRecordPhase(recordPhase);
		migrationAuditDataRequest.setRecordPhaseStatus(recordPhaseStatus);

		return migrationAuditDataRequest;
	}

	private CarbonCopy createCarbonCopy() {

		CarbonCopy carbonCopy = new CarbonCopy();
		carbonCopy.setName(ccName);
		carbonCopy.setEmail(ccEmail);
		carbonCopy.setRecipientId(CustomBooleanEditor.VALUE_1);
		carbonCopy.setRoutingOrder(CustomBooleanEditor.VALUE_1);
		return carbonCopy;
	}

	private Attachment createAttachment(String recordId, List<MigrationProntoDataResponse> migrationSignedDraftList,
			String processId) {

		Attachment attachment = new Attachment();
		attachment.setLabel(MigrationAppConstants.ATTACHMENT_NAME_LABEL + recordId);
		attachment.setAttachmentType(MigrationAppConstants.JSON_EXTENSION);
		attachment.setName(MigrationAppConstants.ATTACHMENT_NAME_LABEL + recordId);
		attachment.setAccessControl(MigrationAppConstants.SENDER_ACCESS_CONTROL);
		try {
			attachment.setData(Base64.getEncoder()
					.encodeToString(objectMapper.writeValueAsString(migrationSignedDraftList).getBytes()));

			log.debug("Attachment successfully created for recordId -> {} in processId {}", recordId, processId);
		} catch (JsonProcessingException e) {

			log.error(
					"JSON Mapping error occured in converting to attachment string for migrationSignedDraftList in createAttachment for recordId {} in processId {}",
					recordId, processId);
			throw new JSONConversionException(
					"JSON Mapping error occured in converting to attachment string for migrationSignedDraftList in createAttachment for recordId "
							+ recordId + " in processId " + processId,
					e.getCause());
		}

		return attachment;
	}
}