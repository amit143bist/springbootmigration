package com.ds.migration.processstart.consumer.listener;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ds.migration.common.constant.FailureCode;
import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.constant.RecordProcessPhase;
import com.ds.migration.common.constant.RecordProcessPhaseStatus;
import com.ds.migration.common.constant.RetryStatus;
import com.ds.migration.common.constant.ValidationResult;
import com.ds.migration.common.exception.AuthenticationTokenException;
import com.ds.migration.common.exception.DocumentNotFoundException;
import com.ds.migration.common.exception.DocumentValidationFailureException;
import com.ds.migration.common.exception.EnvelopeNotCreatedException;
import com.ds.migration.common.exception.InvalidInputException;
import com.ds.migration.common.exception.MissingSignatureDetailsException;
import com.ds.migration.common.exception.URLConnectionException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import com.ds.migration.feign.prontodata.domain.MigrationProntoDataResponse;
import com.ds.migration.feign.prontodata.domain.MigrationSignedDraftDefinition;
import com.ds.migration.feign.prontodata.domain.ProntoDataInformation;
import com.ds.migration.processstart.consumer.client.MigrationProntoDataClient;
import com.ds.migration.processstart.consumer.domain.AsyncRecordData;
import com.ds.migration.processstart.consumer.service.CreateEnvelopeService;
import com.ds.migration.processstart.consumer.service.DSAuthenticationService;
import com.ds.migration.processstart.consumer.service.SendToAuditService;
import com.ds.migration.processstart.consumer.service.SendToFailureService;
import com.ds.migration.processstart.consumer.service.SendToProcessCompleteService;
import com.ds.migration.processstart.consumer.service.SendToSaveDataService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessStartListener extends AbstractMigrationListener<ConcurrentProcessMessageDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("#{'HOLD_' + '${migration.queue.name}'}")
	private String holdQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Value("${migration.docusign.baseuri}")
	private String dsBaseUri;

	@Value("${migration.docusign.accountguid}")
	private String dsAccountGuid;

	@Value("${migration.docusign.envelopeuri}")
	private String dsEnvelopeuri;

	@Value("${migration.docusign.envelopearchiveavailable}")
	private boolean envelopeArchiveAvailable;

	@Value("${migration.application.totalrecordsperqueuemessage}")
	private Integer totalRecordsPerQueueMessage;

	@Value("${migration.application.markprocesscompleteonerror}")
	private boolean markProcessCompleteOnError;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private MigrationProntoDataClient migrationProntoDataClient;

	@Autowired
	private CreateEnvelopeService createEnvelopeService;

	@Autowired
	SendToSaveDataService sendToSaveDataService;

	@Autowired
	private SendToAuditService sendToAuditService;

	@Autowired
	private SendToFailureService sendToFailureService;

	@Autowired
	private SendToProcessCompleteService sendToProcessCompleteService;

	@Autowired
	private DSAuthenticationService dsAuthenticationService;

	@Autowired
	TaskExecutor recordTaskExecutor;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.info("ConcurrentProcessMessageDefinition received in processMessage() -> {} and xDeath value is {}",
				concurrentProcessMessageDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, concurrentProcessMessageDefinition);

	}

	@Override
	protected void callService(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		validateEnvelopeURI();

		try {

			// load token in memory
			dsAuthenticationService.fetchDSAuthToken(concurrentProcessMessageDefinition.getProcessId());// load DS Auth
			fetchSignedDraftDetailsFromPronto(concurrentProcessMessageDefinition);
		} catch (AuthenticationTokenException exp) {

			log.error(
					"Exception {} with message {} occurred in getting accesstoken in ProcessStartListener.callService() for processId {}",
					exp.getCause(), exp.getMessage(), concurrentProcessMessageDefinition.getProcessId());

			rabbitTemplate.convertAndSend(holdQueueName, concurrentProcessMessageDefinition);
		}

	}

	private void fetchSignedDraftDetailsFromPronto(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		log.debug("Inside fetchSignedDraftDetailsFromPronto for processId {}",
				concurrentProcessMessageDefinition.getProcessId());

		CompletableFuture.supplyAsync((Supplier<ProntoDataInformation>) () -> {

			log.info(
					"MigrationProntoDataClient.fetchAllSignedDraftDetails (via supplyAsync) called in fetchSignedDraftDetailsFromPronto for processId {} to fetch signaturedetails",
					concurrentProcessMessageDefinition.getProcessId());

			List<BigInteger> signedDraftIds = concurrentProcessMessageDefinition.getSignedDraftIds();

			List<Long> signedDraftIdList = signedDraftIds.stream().map(BigInteger::longValue)
					.collect(Collectors.toList());

			MigrationSignedDraftDefinition migrationSignedDraftDefinition = new MigrationSignedDraftDefinition();
			migrationSignedDraftDefinition.setSignedDraftIdList(signedDraftIdList);
			migrationSignedDraftDefinition.setProcessId(concurrentProcessMessageDefinition.getProcessId());

			ProntoDataInformation prontoDataInformation = migrationProntoDataClient
					.fetchAllSignedDraftDetails(migrationSignedDraftDefinition).getBody();

			return prontoDataInformation;

		}, recordTaskExecutor).thenApplyAsync(prontoDataInformation -> {

			log.info(
					"DataStatus returned from migrationProntoDataClient.fetchAllSignedDraftDetails is {} for fetchSignedDraftDetailsFromPronto for processId {} ",
					prontoDataInformation.getDataStatus(), concurrentProcessMessageDefinition.getProcessId());

			if (ValidationResult.SOMEORALLFAILED.toString() == prontoDataInformation.getDataStatus()) {

				log.error(
						"################################ Check ProntoData Logs for exception for processId {} ################################",
						concurrentProcessMessageDefinition.getProcessId());
			}

			String dsURL = dsBaseUri + dsAccountGuid + dsEnvelopeuri;
			List<BigInteger> signedDraftIds = concurrentProcessMessageDefinition.getSignedDraftIds();

			List<BigInteger> syncList = Collections.synchronizedList(new ArrayList<BigInteger>(signedDraftIds));

			List<MigrationAuditDataRequest> globalSyncMigrationAuditDataRequestList = Collections
					.synchronizedList(new ArrayList<MigrationAuditDataRequest>());

			List<MigrationRecordIdInformation> globalSyncMigrationRecordIdInformationList = Collections
					.synchronizedList(new ArrayList<MigrationRecordIdInformation>());

			List<BigInteger> globalFailedSignedDraftIds = Collections.synchronizedList(new ArrayList<BigInteger>());

			// Create envelope for each record asynchronously
			signedDraftIds.stream()
					.map(signedDraftId -> createDSRecordFromPronto(dsURL, concurrentProcessMessageDefinition,
							signedDraftId, syncList, globalSyncMigrationAuditDataRequestList,
							globalSyncMigrationRecordIdInformationList,
							prontoDataInformation.getProntoDataSignedDraftResponseMap(), globalFailedSignedDraftIds))
					.collect(Collectors.toList());

			return prontoDataInformation.getDataStatus();

		}, recordTaskExecutor).handleAsync((prontoDataApplyResult, exp) -> {

			log.info(
					"ProntoDataApplyResult in fetchSignedDraftDetailsFromPronto is {} for processId -> {} in handleAsync() ",
					prontoDataApplyResult, concurrentProcessMessageDefinition.getProcessId());

			if (null != prontoDataApplyResult) {

				log.info("No Exception occurred in fetchSignedDraftDetailsFromPronto for processId ->{}!!",
						concurrentProcessMessageDefinition.getProcessId());
			} else {

				log.error(
						"**************************************************************** Got Exception -> {} in fetchSignedDraftDetailsFromPronto for processId -> {}, check logs for more details ****************************************************************",
						exp.getMessage(), concurrentProcessMessageDefinition.getProcessId(),
						concurrentProcessMessageDefinition.getProcessId());
				exp.printStackTrace();

				log.error(
						"{} is thrown in fetchSignedDraftDetailsFromPronto and exception message is {} in processing processId {} for batchId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
						exp.getCause(), exp.getMessage(), concurrentProcessMessageDefinition.getProcessId(),
						concurrentProcessMessageDefinition.getBatchId(), -1, retryLimit,
						HttpStatus.UNPROCESSABLE_ENTITY.toString());

				sendToDeadQueue(concurrentProcessMessageDefinition, HttpStatus.UNPROCESSABLE_ENTITY.toString(),
						exp.getMessage());

			}

			return null;
		}, recordTaskExecutor);

	}

	private CompletableFuture<AsyncRecordData> createDSRecordFromPronto(String dsURL,
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition, BigInteger recordId,
			List<BigInteger> syncList, List<MigrationAuditDataRequest> globalSyncMigrationAuditDataRequestList,
			List<MigrationRecordIdInformation> globalSyncMigrationRecordIdInformationList,
			Map<Long, MigrationProntoDataResponse[]> prontoDataSignedDraftResponseMap,
			List<BigInteger> globalFailedSignedDraftIds) {

		log.info("CreateDSRecordFromPronto is called for recordId {} and processId -> {}", recordId,
				concurrentProcessMessageDefinition.getProcessId());

		return CompletableFuture.supplyAsync((Supplier<AsyncRecordData>) () -> {

			log.info(
					"SupplyAsync called in createDSRecordFromPronto for recordId {} in processId {} to sort and Call EnvelopeService",
					recordId.longValue(), concurrentProcessMessageDefinition.getProcessId());

			List<MigrationProntoDataResponse> migrationSignedDraftList = null;
			if (null != prontoDataSignedDraftResponseMap
					&& null != prontoDataSignedDraftResponseMap.get(recordId.longValue())) {

				migrationSignedDraftList = Arrays.asList(prontoDataSignedDraftResponseMap.get(recordId.longValue()));
			}

			String recordIdStr = recordId.toString();
			List<BigInteger> failedSignedDraftIds = new ArrayList<BigInteger>();
			List<MigrationAuditDataRequest> migrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>();
			List<MigrationRecordIdInformation> migrationRecordIdInformationList = new ArrayList<MigrationRecordIdInformation>();

			sortSignedDraftListAndCallEnvelopeService(dsURL, concurrentProcessMessageDefinition, recordId,
					migrationSignedDraftList, recordIdStr, failedSignedDraftIds, migrationAuditDataRequestList,
					migrationRecordIdInformationList);

			AsyncRecordData asyncRecordData = new AsyncRecordData();

			asyncRecordData.setFailedSignedDraftIds(failedSignedDraftIds);
			asyncRecordData.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
			asyncRecordData.setMigrationRecordIdInformationList(migrationRecordIdInformationList);

			return asyncRecordData;
		}, recordTaskExecutor).thenApplyAsync(asyncRecordData -> {

			applyAsyncRecordData(concurrentProcessMessageDefinition, recordId, syncList, asyncRecordData,
					globalSyncMigrationAuditDataRequestList, globalSyncMigrationRecordIdInformationList,
					globalFailedSignedDraftIds);

			if (asyncRecordData.getFailedSignedDraftIds().isEmpty()) {
				return ValidationResult.SUCCESS;
			}

			return ValidationResult.FAILED;

		}, recordTaskExecutor).handleAsync((asyncRecordDataApplyResult, exp) -> {

			log.info(
					"AsyncRecordDataApplyResult in createDSRecordFromPronto is {} for recordId -> {} and processId -> {} in handleAsync() ",
					asyncRecordDataApplyResult, recordId, concurrentProcessMessageDefinition.getProcessId());

			if (null != asyncRecordDataApplyResult) {

				log.debug("No Exception occurred in createDSRecordFromPronto for recordId ->{} and processId -> {}!!",
						recordId, concurrentProcessMessageDefinition.getProcessId());
			} else {

				log.error(
						"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Got Exception -> {} in createDSRecordFromPronto for recordId -> {} and processId -> {}, check logs for more details $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$",
						exp.getMessage(), recordId, concurrentProcessMessageDefinition.getProcessId());

				exp.printStackTrace();

				createFailureMessage(concurrentProcessMessageDefinition, recordId.toString(),
						FailureCode.ERROR_106.toString(), FailureCode.ERROR_107.getFailureCodeDescription(),
						RecordProcessPhase.FETCH_SIGNATURE_DETAILS.toString(),
						new MissingSignatureDetailsException("migrationSignedDraftList is empty or null for recordId "
								+ recordId + " and processId " + concurrentProcessMessageDefinition.getProcessId()));
			}

			return null;
		}, recordTaskExecutor);

	}

	private void sortSignedDraftListAndCallEnvelopeService(String dsURL,
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition, BigInteger recordId,
			List<MigrationProntoDataResponse> migrationSignedDraftList, String recordIdStr,
			List<BigInteger> failedSignedDraftIds, List<MigrationAuditDataRequest> migrationAuditDataRequestList,
			List<MigrationRecordIdInformation> migrationRecordIdInformationList) {

		if (null != migrationSignedDraftList && !migrationSignedDraftList.isEmpty()) {

			migrationAuditDataRequestList.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(),
					recordIdStr, RecordProcessPhase.FETCH_SIGNATURE_DETAILS.toString(),
					RecordProcessPhaseStatus.S.toString()));

			// sort migrationSignedDraftList with desc documentid
			log.debug("MigrationSignedDraftList before sorting is {}", migrationSignedDraftList);

			migrationSignedDraftList.sort(Comparator.comparing(MigrationProntoDataResponse::getDocumentid));

			log.debug("MigrationSignedDraftList after sorting is {}", migrationSignedDraftList);

			callEnvelopeServiceToCreateEnvelope(concurrentProcessMessageDefinition, dsURL, failedSignedDraftIds,
					recordId, migrationSignedDraftList, recordIdStr, migrationAuditDataRequestList,
					migrationRecordIdInformationList);
		} else {

			log.error(
					"MigrationSignedDraftList is empty or null in ProcessStartListener.callService() for recordId -> {} and processId {}, sending AuditRequest, check ProntoData Logs for more details",
					recordIdStr, concurrentProcessMessageDefinition.getProcessId());
			failedSignedDraftIds.add(recordId);
			migrationAuditDataRequestList.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(),
					recordIdStr, RecordProcessPhase.FETCH_SIGNATURE_DETAILS.toString(),
					RecordProcessPhaseStatus.F.toString()));

			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_107.toString(),
					FailureCode.ERROR_107.getFailureCodeDescription(),
					RecordProcessPhase.FETCH_SIGNATURE_DETAILS.toString(),
					new MissingSignatureDetailsException("migrationSignedDraftList is empty or null for recordId "
							+ recordIdStr + " and processId " + concurrentProcessMessageDefinition.getProcessId()));
		}
	}

	private synchronized void applyAsyncRecordData(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition, BigInteger recordId,
			List<BigInteger> syncList, AsyncRecordData asyncRecordData,
			List<MigrationAuditDataRequest> globalSyncMigrationAuditDataRequestList,
			List<MigrationRecordIdInformation> globalSyncMigrationRecordIdInformationList,
			List<BigInteger> globalFailedSignedDraftIds) {

		log.info("Async process finishing in applyAsyncRecordData for recordId -> {} and processId {}", recordId,
				concurrentProcessMessageDefinition.getProcessId());

		List<BigInteger> failedSignedDraftIdList = asyncRecordData.getFailedSignedDraftIds();

		if (failedSignedDraftIdList.isEmpty() || markProcessCompleteOnError) {

			syncList.remove(recordId);

			if (null != concurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap()
					&& !concurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty()) {

				createFailureMessageRetrySuccess(concurrentProcessMessageDefinition, String.valueOf(recordId), null,
						null, null, null);
			}

		} else {

			failedSignedDraftIdList.forEach(failedSignedDraftId -> log.error(
					"FailedSignedDraftId in applyAsyncRecordData is -> {} for recordId -> {} and processId -> {}",
					failedSignedDraftId, recordId, concurrentProcessMessageDefinition.getProcessId()));
			globalFailedSignedDraftIds.addAll(failedSignedDraftIdList);
		}

		globalSyncMigrationAuditDataRequestList.addAll(asyncRecordData.getMigrationAuditDataRequestList());
		globalSyncMigrationRecordIdInformationList.addAll(asyncRecordData.getMigrationRecordIdInformationList());

		if (globalSyncMigrationAuditDataRequestList.size() >= totalRecordsPerQueueMessage || syncList.isEmpty()
				|| !globalFailedSignedDraftIds.isEmpty()) {

			// Shallow copy
			List<MigrationAuditDataRequest> sendSplitSyncMigrationAuditDataRequestList = new ArrayList<MigrationAuditDataRequest>(
					globalSyncMigrationAuditDataRequestList);
			CompletableFuture.runAsync(() -> {

				sendToAuditService.sendAuditMessage(sendSplitSyncMigrationAuditDataRequestList,
						concurrentProcessMessageDefinition.getProcessId());

			}, recordTaskExecutor);

			globalSyncMigrationAuditDataRequestList.clear();
		}

		if (globalSyncMigrationRecordIdInformationList.size() >= totalRecordsPerQueueMessage || syncList.isEmpty()
				|| !globalFailedSignedDraftIds.isEmpty()) {

			// Shallow copy
			List<MigrationRecordIdInformation> sendSplitSyncMigrationRecordIdInformationList = new ArrayList<MigrationRecordIdInformation>(
					globalSyncMigrationRecordIdInformationList);
			CompletableFuture.runAsync(() -> {

				sendToSaveDataService.sendSaveDataMessage(sendSplitSyncMigrationRecordIdInformationList,
						concurrentProcessMessageDefinition.getProcessId());

			}, recordTaskExecutor);

			globalSyncMigrationRecordIdInformationList.clear();
		}

		checkAndSendProcessCompletionMessage(concurrentProcessMessageDefinition, recordId, syncList,
				globalFailedSignedDraftIds);

	}

	private void checkAndSendProcessCompletionMessage(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition, BigInteger recordId,
			List<BigInteger> syncList, List<BigInteger> globalFailedSignedDraftIds) {

		log.info("SyncList size in checkAndSendProcessCompletionMessage is {} for recordId {} and processId {}",
				syncList.size(), recordId, concurrentProcessMessageDefinition.getProcessId());

		synchronized (syncList) {

			if (syncList.isEmpty()) {

				log.info(
						"SyncList size is empty in checkAndSendProcessCompletionMessage for lastRecordId is {} and processId -> {}",
						recordId, concurrentProcessMessageDefinition.getProcessId());

				synchronized (globalFailedSignedDraftIds) {

					if (globalFailedSignedDraftIds.isEmpty() || markProcessCompleteOnError) {

						log.info(
								"Either globalFailedSignedDraftIds size is empty or markProcessCompleteOnErrorvalue is {} in checkAndSendProcessCompletionMessage for lastRecordId is {} and processId -> {}",
								markProcessCompleteOnError, recordId,
								concurrentProcessMessageDefinition.getProcessId());

						log.info(
								"All asyncJobs are completed successfully in checkAndSendProcessCompletionMessage for lastRecordId is {} and processId -> {}",
								recordId, concurrentProcessMessageDefinition.getProcessId());

						CompletableFuture.runAsync(() -> {

							log.info(
									"Calling ProcessCompleteService to mark the proccess for completion in checkAndSendProcessCompletionMessage for processId -> {} and batchId -> {}",
									concurrentProcessMessageDefinition.getProcessId(),
									concurrentProcessMessageDefinition.getBatchId());

							callSendToProcessCompleteService(concurrentProcessMessageDefinition);

						}, recordTaskExecutor);
					}
				}

			}
		}
	}

	private void callSendToProcessCompleteService(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(concurrentProcessMessageDefinition.getBatchId());
		concurrentProcessLogDefinition.setProcessId(concurrentProcessMessageDefinition.getProcessId());
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.COMPLETED.toString());

		sendToProcessCompleteService.sendProcessCompleteMessage(concurrentProcessLogDefinition);
	}

	private void callEnvelopeServiceToCreateEnvelope(
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition, String dsURL,
			List<BigInteger> failedSignedDraftIds, BigInteger recordId,
			List<MigrationProntoDataResponse> migrationSignedDraftList, String recordIdStr,
			List<MigrationAuditDataRequest> migrationAuditDataRequestList,
			List<MigrationRecordIdInformation> migrationRecordIdInformationList) {

		try {

			// create envelope
			String envelopeId = createEnvelopeService.createEnvelope(migrationSignedDraftList, dsURL,
					concurrentProcessMessageDefinition.getProcessId(), recordIdStr, envelopeArchiveAvailable,
					migrationAuditDataRequestList);

			log.debug(
					"EnvelopeId created for recordId {} and processId {} in callEnvelopeServiceToCreateEnvelope() is {}",
					recordId, concurrentProcessMessageDefinition.getProcessId(), envelopeId);

			migrationAuditDataRequestList.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(),
					recordIdStr, RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(),
					RecordProcessPhaseStatus.S.toString()));

			migrationRecordIdInformationList.add(new MigrationRecordIdInformation(recordIdStr, envelopeId));

			migrationAuditDataRequestList
					.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(), recordIdStr,
							RecordProcessPhase.POST_DS_ID_ORACLE.toString(), RecordProcessPhaseStatus.S.toString()));

		} catch (DocumentValidationFailureException exp) {

			failedSignedDraftIds.add(recordId);

			migrationAuditDataRequestList
					.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(), recordIdStr,
							RecordProcessPhase.PRONTO_DOC_VERIFY.toString(), RecordProcessPhaseStatus.F.toString()));

			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_101.toString(),
					FailureCode.ERROR_101.getFailureCodeDescription(), RecordProcessPhase.PRONTO_DOC_VERIFY.toString(),
					exp);

		} catch (DocumentNotFoundException exp) {

			failedSignedDraftIds.add(recordId);
			migrationAuditDataRequestList
					.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(), recordIdStr,
							RecordProcessPhase.FETCH_PRONTO_DOC.toString(), RecordProcessPhaseStatus.F.toString()));

			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_102.toString(),
					FailureCode.ERROR_102.getFailureCodeDescription(), RecordProcessPhase.FETCH_PRONTO_DOC.toString(),
					exp);
		} catch (EnvelopeNotCreatedException exp) {

			failedSignedDraftIds.add(recordId);
			migrationAuditDataRequestList.add(createAuditRequest(concurrentProcessMessageDefinition.getProcessId(),
					recordIdStr, RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(),
					RecordProcessPhaseStatus.F.toString()));

			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_103.toString(),
					FailureCode.ERROR_103.getFailureCodeDescription(),
					RecordProcessPhase.CREATE_DOCUSIGN_ARTIFACT.toString(), exp);
		} catch (ResponseStatusException exp) {

			failedSignedDraftIds.add(recordId);

			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_104.toString(),
					FailureCode.ERROR_104.getFailureCodeDescription(), exp.getReason(), exp);
		} catch (URLConnectionException exp) {

			failedSignedDraftIds.add(recordId);
			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_105.toString(),
					FailureCode.ERROR_105.getFailureCodeDescription(), exp.getMessage(), exp);
		} catch (Exception exp) {

			failedSignedDraftIds.add(recordId);
			createFailureMessage(concurrentProcessMessageDefinition, recordIdStr, FailureCode.ERROR_106.toString(),
					FailureCode.ERROR_106.getFailureCodeDescription(), exp.getMessage(), exp);
		}
	}

	private void validateEnvelopeURI() {

		if (!envelopeArchiveAvailable && dsEnvelopeuri.contains(MigrationAppConstants.ENVELOPE_ARCHIVE_PATH)) {

			log.error(
					" <<<<<<< Wrong property set >>>>>>> envelopeArchiveAvailable is {}, and envelopeuri set in properties is {}",
					envelopeArchiveAvailable, dsEnvelopeuri);
			throw new InvalidInputException(
					"Wrong property set for envelopeuri, envelopeuri cannot have uri with /envelopearchive");
		}

		if (envelopeArchiveAvailable && !dsEnvelopeuri.contains("/envelopearchive")) {

			log.error(
					" <<<<<<< Wrong property set >>>>>>> envelopeArchiveAvailable is {}, and envelopeuri set in properties is {}",
					envelopeArchiveAvailable, dsEnvelopeuri);
			throw new InvalidInputException(
					"Wrong property set for envelopeuri, envelopeuri must have uri with /envelopearchive");
		}
	}

	private void createFailureMessage(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition,
			String recordIdStr, String failureCode, String failureReason, String failureStep, Exception exp) {

		log.error(
				"Failure occurred for recordId -> {} and processId {} with failureCode -> {}, failureReason -> {}, exceptionMessage is {} and cause is {}",
				recordIdStr, concurrentProcessMessageDefinition.getProcessId(), failureCode, failureReason,
				exp.getMessage(), exp.getCause());

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition();
		concurrentProcessFailureLogDefinition.setFailureCode(failureCode);
		concurrentProcessFailureLogDefinition.setFailureReason(failureReason);
		concurrentProcessFailureLogDefinition
				.setFailureDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		concurrentProcessFailureLogDefinition.setFailureRecordId(recordIdStr);
		concurrentProcessFailureLogDefinition.setFailureStep(failureStep);
		concurrentProcessFailureLogDefinition.setProcessId(concurrentProcessMessageDefinition.getProcessId());

		Map<String, String> failureIdProcessFailureIdMap = concurrentProcessMessageDefinition
				.getFailureIdProcessFailureIdMap();
		if (null != failureIdProcessFailureIdMap && !failureIdProcessFailureIdMap.isEmpty()) {

			concurrentProcessFailureLogDefinition.setProcessFailureId(failureIdProcessFailureIdMap.get(recordIdStr));
			concurrentProcessFailureLogDefinition.setRetryStatus(RetryStatus.F.toString());

		}

		sendToFailureService.sendFailureMessage(concurrentProcessFailureLogDefinition);
	}

	private void createFailureMessageRetrySuccess(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition,
			String recordIdStr, String failureCode, String failureReason, String failureStep, Exception exp) {

		log.error("Retry succcess occurred for recordId -> {} and processId {}", recordIdStr,
				concurrentProcessMessageDefinition.getProcessId());

		ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition = new ConcurrentProcessFailureLogDefinition();
		concurrentProcessFailureLogDefinition
				.setSuccessDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		concurrentProcessFailureLogDefinition.setFailureRecordId(recordIdStr);
		concurrentProcessFailureLogDefinition.setProcessId(concurrentProcessMessageDefinition.getProcessId());

		Map<String, String> failureIdProcessFailureIdMap = concurrentProcessMessageDefinition
				.getFailureIdProcessFailureIdMap();
		if (null != concurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap()
				&& !concurrentProcessMessageDefinition.getFailureIdProcessFailureIdMap().isEmpty()) {

			concurrentProcessFailureLogDefinition.setProcessFailureId(failureIdProcessFailureIdMap.get(recordIdStr));
			concurrentProcessFailureLogDefinition.setRetryStatus(RetryStatus.S.toString());
		}

		sendToFailureService.sendFailureMessage(concurrentProcessFailureLogDefinition);
	}

	private MigrationAuditDataRequest createAuditRequest(String processId, String recordId, String recordPhase,
			String recordPhaseStatus) {

		log.debug(
				"Audit Request sent for recordId -> {} and processId {} with recordPhase -> {} and recordPhaseStatus -> {}",
				recordId, processId, recordPhase, recordPhaseStatus);

		MigrationAuditDataRequest migrationAuditDataRequest = new MigrationAuditDataRequest();
		migrationAuditDataRequest.setAuditEntryDateTime(MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
		migrationAuditDataRequest.setProcessId(processId);
		migrationAuditDataRequest.setRecordId(recordId);
		migrationAuditDataRequest.setRecordPhase(recordPhase);
		migrationAuditDataRequest.setRecordPhaseStatus(recordPhaseStatus);

		return migrationAuditDataRequest;
	}

	@Override
	protected void sendToDeadQueue(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition,
			String httpStatus, String errorHeaderMessage) {

		log.error("Message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {} for processId {}",
				concurrentProcessMessageDefinition, errorHeaderMessage,
				concurrentProcessMessageDefinition.getProcessId());

		rabbitTemplate.convertAndSend(deadQueueName, concurrentProcessMessageDefinition, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	protected void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		log.error(
				"{} is thrown and exception message is {} in processing processId {} for batchId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), concurrentProcessMessageDefinition.getProcessId(),
				concurrentProcessMessageDefinition.getBatchId(), retryCount, retryLimit, httpStatus);
	}

}