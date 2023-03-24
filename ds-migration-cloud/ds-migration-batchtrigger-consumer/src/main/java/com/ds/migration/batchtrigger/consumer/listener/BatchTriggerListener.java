package com.ds.migration.batchtrigger.consumer.listener;

import com.ds.migration.batchtrigger.consumer.client.CoreScheduledBatchLogClient;
import com.ds.migration.batchtrigger.consumer.service.BatchDataService;
import com.ds.migration.batchtrigger.consumer.service.ProntoDataService;
import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.exception.RunningBatchException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.batch.domain.BatchStartParams;
import com.ds.migration.feign.batch.domain.MigrationBatchTriggerInformation;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BatchTriggerListener extends AbstractMigrationListener<MigrationBatchTriggerInformation> {
	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@Autowired
	BatchDataService batchDataProcessor;

	@Autowired
	ProntoDataService prontoDataProcessor;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(MigrationBatchTriggerInformation migrationBatchTriggerInformation,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.info("MigrationBatchTriggerInformation received in processMessage() -> {} and xDeath value is {}",
				migrationBatchTriggerInformation, xDeath);

		super.processMessage(xDeath, retryLimit, migrationBatchTriggerInformation);

	}

	@Override
	protected void callService(MigrationBatchTriggerInformation migrationBatchTriggerInformation) {

		BatchStartParams batchStartParams = null;
		try {
			ResponseEntity<ScheduledBatchLogResponse> scheduledBatchLogResponseEntity = coreScheduledBatchLogClient
					.findLatestBatchByBatchType(migrationBatchTriggerInformation.getBatchType());

			ScheduledBatchLogResponse scheduledBatchLogResponse = scheduledBatchLogResponseEntity.getBody();

			if (null != scheduledBatchLogResponse.getBatchEndDateTime()) {

				log.info("Successfully found last completed batch job of batchType -> {}, last completed batchId is {}",
						migrationBatchTriggerInformation.getBatchType(), scheduledBatchLogResponse.getBatchId());

				batchStartParams = batchDataProcessor.calculateBatchTriggerParameters(scheduledBatchLogResponse,
						migrationBatchTriggerInformation);

			} else {

				log.error("Another Batch running of batchType -> {} since {}",
						migrationBatchTriggerInformation.getBatchType(),
						scheduledBatchLogResponse.getBatchStartDateTime());

				throw new RunningBatchException("Another Batch already running for batch type "
						+ migrationBatchTriggerInformation.getBatchType() + " since "
						+ scheduledBatchLogResponse.getBatchStartDateTime());
			}

		} catch (ResponseStatusException exp) {

			// Below code should run only once, when batch will be triggered first time
			log.info("No Batch running of batchType -> {}", migrationBatchTriggerInformation.getBatchType());
			if (exp.getStatus() == HttpStatus.NOT_FOUND) {

				batchStartParams = new BatchStartParams(migrationBatchTriggerInformation.getBatchStartDateTime(),
						migrationBatchTriggerInformation.getBatchEndDateTime(),
						migrationBatchTriggerInformation.getNumberOfRecordsPerThread());

			}
		}

		startBatchWork(migrationBatchTriggerInformation, batchStartParams);
	}

	private void startBatchWork(MigrationBatchTriggerInformation migrationBatchTriggerInformation,
			BatchStartParams batchStartParams) {

		log.info("batchStartParams in startBatchWork() are {}", batchStartParams);
		List<BigInteger> signedDraftIds = prontoDataProcessor.fetchProntoData(batchStartParams);

		// remove duplicates
		List<BigInteger> signedDraftIdsWithoutDuplicates = signedDraftIds.stream().distinct()
				.collect(Collectors.toList());

		String batchId = batchDataProcessor.createBatchJob(migrationBatchTriggerInformation.getBatchType(),
				batchStartParams, BigInteger.valueOf(signedDraftIdsWithoutDuplicates.size()));

		batchDataProcessor.sendSignedDraftIdsForProcessing(signedDraftIdsWithoutDuplicates, batchStartParams, batchId);
	}

	@Override
	protected void sendToDeadQueue(MigrationBatchTriggerInformation migrationBatchTriggerInformation, String httpStatus,
			String errorHeaderMessage) {

		log.error("message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {}",
				migrationBatchTriggerInformation, errorHeaderMessage);

		rabbitTemplate.convertAndSend(deadQueueName, migrationBatchTriggerInformation, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	protected void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			MigrationBatchTriggerInformation migrationBatchTriggerInformation) {

		log.error(
				"{} is thrown and exception message is {} in processing batchType {} and batchstartdatetime {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), migrationBatchTriggerInformation.getBatchType(),
				migrationBatchTriggerInformation.getBatchStartDateTime(), retryCount, retryLimit, httpStatus);
	}

}