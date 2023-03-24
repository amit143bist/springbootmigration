package com.ds.migration.prontodata.controller;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.common.exception.AsyncInterruptedException;
import com.ds.migration.common.exception.ResourceNotFoundException;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataRequest;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataResponse;
import com.ds.migration.feign.prontodata.domain.MigrationSignedDraftDefinition;
import com.ds.migration.feign.prontodata.domain.ProntoDataInformation;
import com.ds.migration.feign.prontodata.domain.SignedDraftInformation;
import com.ds.migration.feign.prontodata.service.MigrationProntoDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
@RolesAllowed("USER")
@Slf4j
public class MigrationProntoDataController implements MigrationProntoDataService {

	@Value("${pronto.service.baseurl}")
	private String prontoBaseURL;

	@Value("${oracle.ibis.tokentype}")
	private String ibisTokenType;

	@Value("${oracle.ibis.accesstoken}")
	private String ibisAccessToken;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private HttpHeaders headers;

	@Autowired
	TaskExecutor prontoTaskExecutor;

	@Override
	public ResponseEntity<BigInteger[]> signedDraftList(LocalDateTime beginDateTime, LocalDateTime endDateTime) {

		log.info("MigrationProntodataController.signedDraftList() beginDateTime -> {}, endDateTime -> {}",
				beginDateTime, endDateTime);

		log.info("Params to call IBIS APIs with beginDateTimewithZ offset -> {}, endDateTimewithZ offset -> {}",
				beginDateTime.toInstant(ZoneOffset.UTC), endDateTime.toInstant(ZoneOffset.UTC));

		return Optional.ofNullable(
				restTemplate.exchange(prontoBaseURL + "/signeddraft?begindate={beginDateTime}&enddate={endDateTime}",
						HttpMethod.GET, prepareHTTPEntity(), SignedDraftInformation[].class,
						beginDateTime.toInstant(ZoneOffset.UTC), endDateTime.toInstant(ZoneOffset.UTC)))
				.map(prontoSignedDrafts -> {

					log.info("prontoSignedDrafts are {}", prontoSignedDrafts);
					Assert.notEmpty(prontoSignedDrafts.getBody(), "ProntoSignedDrafts is null for beginDateTime "
							+ beginDateTime + " endDateTime " + endDateTime);
					Assert.isTrue(prontoSignedDrafts.getStatusCode().is2xxSuccessful(),
							"ProntoSignedDrafts is not returned with 200 status code");

					List<SignedDraftInformation> prontoSignedDraftsList = Arrays.asList(prontoSignedDrafts.getBody());

					log.info("prontoSignedDraftsList elements are {}", prontoSignedDraftsList);

					BigInteger[] signedDraftBigIntArr = prontoSignedDraftsList.stream()
							.map(SignedDraftInformation::getSignedDraftId).toArray(BigInteger[]::new);

					return new ResponseEntity<BigInteger[]>(signedDraftBigIntArr, HttpStatus.OK);

				}).orElseThrow(() -> new ResourceNotFoundException("Pronto entries not found for for beginDateTime "
						+ beginDateTime + " endDateTime " + endDateTime));

	}

	private HttpEntity<String> prepareHTTPEntity() {

		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", ibisTokenType + " " + ibisAccessToken);

		return new HttpEntity<>(headers);
	}

	@Override
	public ResponseEntity<MigrationProntoDataResponse[]> signedDraft(Long id) {

		log.info("signedDraft called for signedDraftId", id);
		return Optional.ofNullable(restTemplate.exchange(prontoBaseURL + "/prontodoc/signeddraftid/{id}",
				HttpMethod.GET, prepareHTTPEntity(), MigrationProntoDataResponse[].class, id))
				.map(prontoSignedDraft -> {

					Assert.isTrue(prontoSignedDraft.getStatusCode().is2xxSuccessful(),
							"prontoSignedDraft is not returned with 200 status code for signedDraftId# " + id);

					return new ResponseEntity<MigrationProntoDataResponse[]>(prontoSignedDraft.getBody(),
							HttpStatus.OK);
				}).orElseThrow(
						() -> new ResourceNotFoundException("Pronto data exception thrown for signedDraftId# " + id));
	}

	@Override
	public ResponseEntity<ProntoDataInformation> fetchAllSignedDraftDetails(
			MigrationSignedDraftDefinition migrationSignedDraftDefinition) {

		log.info("MigrationProntoDataController.fetchAllSignedDraftDetails called for processId {}",
				migrationSignedDraftDefinition.getProcessId());

		List<Long> signedDraftIdList = migrationSignedDraftDefinition.getSignedDraftIdList();

		Map<Long, MigrationProntoDataResponse[]> synchronizedProntoDataSignedDraftResponseMap = Collections
				.synchronizedMap(new HashMap<Long, MigrationProntoDataResponse[]>(signedDraftIdList.size()));

		List<CompletableFuture<MigrationProntoDataResponse[]>> prontoDataInformationFutureList = signedDraftIdList
				.stream()
				.map(signedDraftId -> fetchSignedDraftDetailsAsync(signedDraftId,
						synchronizedProntoDataSignedDraftResponseMap, migrationSignedDraftDefinition))
				.collect(Collectors.toList());

		log.debug("MigrationProntoDataController.fetchAllSignedDraftDetails StartDateTime " + LocalDateTime.now());

		// Create a combined Future using allOf()
		try {

			CompletableFuture.allOf(prontoDataInformationFutureList
					.toArray(new CompletableFuture[prontoDataInformationFutureList.size()])).get();

		} catch (InterruptedException exp) {

			log.error(
					"InterruptedException {} occurred in MigrationProntoDataController.fetchAllSignedDraftDetails for processId {}",
					migrationSignedDraftDefinition.getProcessId());
			exp.printStackTrace();

			throw new AsyncInterruptedException("InterruptedException " + exp
					+ " occurred in MigrationProntoDataController.fetchAllSignedDraftDetails for processId "
					+ migrationSignedDraftDefinition.getProcessId() + " message " + exp.getMessage());
		} catch (ExecutionException exp) {

			log.error(
					"ExecutionException {} occurred in MigrationProntoDataController.fetchAllSignedDraftDetails for processId {}",
					migrationSignedDraftDefinition.getProcessId());
			exp.printStackTrace();

			throw new AsyncInterruptedException("ExecutionException " + exp
					+ " occurred in MigrationProntoDataController.fetchAllSignedDraftDetails for processId "
					+ migrationSignedDraftDefinition.getProcessId() + " message " + exp.getMessage());
		}

		log.debug("MigrationProntoDataController.fetchAllSignedDraftDetails EndDateTime " + LocalDateTime.now());

		ProntoDataInformation prontoDataInformation = new ProntoDataInformation();

		if (null != synchronizedProntoDataSignedDraftResponseMap
				&& !synchronizedProntoDataSignedDraftResponseMap.isEmpty()) {

			prontoDataInformation.setProntoDataSignedDraftResponseMap(synchronizedProntoDataSignedDraftResponseMap);

			int prontoDataSignedDraftResponseMapSize = synchronizedProntoDataSignedDraftResponseMap.size();

			if (prontoDataSignedDraftResponseMapSize != signedDraftIdList.size()) {

				log.error(
						"prontoDataSignedDraftResponseMapSize size {} not equal to signedDraftIdList size {}, check logs for errors",
						prontoDataSignedDraftResponseMapSize, signedDraftIdList.size());

				prontoDataInformation.setDataStatus(ValidationResult.SOMEORALLFAILED.toString());
			} else {

				prontoDataInformation.setDataStatus(ValidationResult.SUCCESS.toString());
			}

		}

		return new ResponseEntity<ProntoDataInformation>(prontoDataInformation, HttpStatus.OK);
	}

	private CompletableFuture<MigrationProntoDataResponse[]> fetchSignedDraftDetailsAsync(Long signedDraftId,
			Map<Long, MigrationProntoDataResponse[]> prontoDataSignedDraftResponseMap,
			MigrationSignedDraftDefinition migrationSignedDraftDefinition) {

		return CompletableFuture.supplyAsync((Supplier<MigrationProntoDataResponse[]>) () -> {

			MigrationProntoDataResponse[] prontoSignedDraftData = restTemplate
					.exchange(prontoBaseURL + "/prontodoc/signeddraftid/{id}", HttpMethod.GET, prepareHTTPEntity(),
							MigrationProntoDataResponse[].class, signedDraftId)
					.getBody();

			return prontoSignedDraftData;

		}, prontoTaskExecutor).handleAsync((prontoSignedDraftData, ex) -> {

			log.info("prontoSignedDraftData in handleAsync() is processed for signedDraftId -> {} and processId -> {} ",
					signedDraftId, migrationSignedDraftDefinition.getProcessId());

			if (null != prontoSignedDraftData) {

				log.info(
						"No Exception occurred in fetchSignedDraftDetailsAsync for signedDraftId ->{} and processId -> {}!!",
						signedDraftId, migrationSignedDraftDefinition.getProcessId());
				prontoDataSignedDraftResponseMap.put(signedDraftId, prontoSignedDraftData);
			} else {

				log.error(
						"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Got Exception -> {} in fetchSignedDraftDetailsAsync for signedDraftId -> {} and processId -> {}, check logs for more details $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$",
						ex.getMessage(), signedDraftId, migrationSignedDraftDefinition.getProcessId());

				ex.printStackTrace();
			}

			return prontoSignedDraftData;
		}, prontoTaskExecutor);
	}

	@Override
	public ResponseEntity<String> updateSignedDraftWithDocuSignId(MigrationProntoDataRequest request, Long id) {

		HttpHeaders httpHeadersLocal = getHeaders();
		try {

			String msgBody = objectMapper.writeValueAsString(new MigrationProntoDataRequest(request.getEnvelopeid()));
			HttpEntity<String> requestEntity = new HttpEntity<String>(msgBody, httpHeadersLocal);
			return Optional.ofNullable(restTemplate.exchange(prontoBaseURL + "/prontodoc/signeddraftid/{id}",
					HttpMethod.PUT, requestEntity, String.class, id)).map(prontoSignedDraft -> {

						Assert.isTrue(prontoSignedDraft.getStatusCode().is2xxSuccessful(),
								"200 status code is not returned for signedDraftId# " + id);

						return new ResponseEntity<String>(prontoSignedDraft.getBody(), HttpStatus.OK);
					}).orElseThrow(() -> new ResourceNotFoundException(
							"Pronto data exception thrown for signedDraftId# " + id));
		} catch (JsonProcessingException e) {

			e.printStackTrace();
		}

		return null;
	}

	private HttpHeaders getHeaders() {

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", ibisTokenType + " " + ibisAccessToken);

		return headers;
	}

	@Override
	public ResponseEntity<MigrationRecordIdInformationDefinition> updateAllSignedDraftWithDocuSignId(
			MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition) {

		List<MigrationRecordIdInformation> migrationRecordIdInformationList = migrationRecordIdInformationDefinition
				.getMigrationRecordIdInformationList();

		String msgBody = null;
		HttpEntity<String> requestEntity = null;
		HttpHeaders httpHeadersLocal = getHeaders();
		List<MigrationRecordIdInformation> failedMigrationAuditDataRequestList = new ArrayList<MigrationRecordIdInformation>();

		for (MigrationRecordIdInformation migrationRecordIdInformation : migrationRecordIdInformationList) {

			try {

				msgBody = objectMapper.writeValueAsString(
						new MigrationProntoDataRequest(migrationRecordIdInformation.getDocuSignId()));
				requestEntity = new HttpEntity<String>(msgBody, httpHeadersLocal);

				restTemplate.exchange(prontoBaseURL + "/prontodoc/signeddraftid/{id}", HttpMethod.PUT, requestEntity,
						String.class, migrationRecordIdInformation.getRecordId());
			} catch (HttpClientErrorException exp) {

				failedMigrationAuditDataRequestList.add(migrationRecordIdInformation);
				log.error(
						"HttpClientErrorException {} occurred with exception message {} for signedDraftId {} in updateAllSignedDraftWithDocuSignId",
						exp.getCause(), exp.getMessage(), migrationRecordIdInformation.getRecordId());
				exp.printStackTrace();
			} catch (Exception exp) {

				failedMigrationAuditDataRequestList.add(migrationRecordIdInformation);
				log.error(
						"Exception {} occurred with exception message {} for signedDraftId {} in updateAllSignedDraftWithDocuSignId",
						exp.getCause(), exp.getMessage(), migrationRecordIdInformation.getRecordId());
				exp.printStackTrace();
			}
		}

		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinitionResp = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinitionResp
				.setMigrationRecordIdInformationList(failedMigrationAuditDataRequestList);
		migrationRecordIdInformationDefinitionResp
				.setTotalRecords(BigInteger.valueOf(failedMigrationAuditDataRequestList.size()));
		migrationRecordIdInformationDefinitionResp.setProcessId(migrationRecordIdInformationDefinition.getProcessId());

		return new ResponseEntity<MigrationRecordIdInformationDefinition>(migrationRecordIdInformationDefinitionResp,
				HttpStatus.OK);
	}

}