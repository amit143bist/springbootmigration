package com.ds.migration.batchtrigger.consumer.service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.ds.migration.batchtrigger.consumer.client.CoreConcurrentProcessLogClient;
import com.ds.migration.batchtrigger.consumer.client.CoreScheduledBatchLogClient;
import com.ds.migration.common.constant.ProcessStatus;
import com.ds.migration.common.exception.JSONConversionException;
import com.ds.migration.common.trigger.RunOnceTrigger;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.batch.domain.BatchStartParams;
import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;
import com.ds.migration.feign.batch.domain.MigrationBatchTriggerInformation;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogRequest;
import com.ds.migration.feign.coredata.domain.ScheduledBatchLogResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BatchDataService {

	@Value("${migration.processstartqueue.name}")
	private String processStartQueueName;

	@Value("${migration.processstartqueue.senddelay}")
	private Integer messagesSendDelay;

	@Value("${migration.processstartqueue.messagespergroup}")
	private Integer messagesPerGroup;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@Autowired
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@Autowired
	ThreadPoolTaskScheduler threadPoolTaskScheduler;

	public BatchStartParams calculateBatchTriggerParameters(ScheduledBatchLogResponse scheduledBatchLogResponse,
			MigrationBatchTriggerInformation migrationBatchTriggerInformation) {

		LocalDateTime newBatchStartDateTime = null;
		LocalDateTime newBatchEndDateTime = null;

		if (null != migrationBatchTriggerInformation.getBatchStartDateTime()) {

			if (null != migrationBatchTriggerInformation.getBatchEndDateTime()) {

				newBatchStartDateTime = MigrationDateTimeUtil
						.convertToLocalDateTime(migrationBatchTriggerInformation.getBatchStartDateTime());

				newBatchEndDateTime = MigrationDateTimeUtil
						.convertToLocalDateTime(migrationBatchTriggerInformation.getBatchEndDateTime());
			} else {

				if (null != migrationBatchTriggerInformation.getNumberOfHours()
						&& migrationBatchTriggerInformation.getNumberOfHours() > -1) {

					newBatchStartDateTime = MigrationDateTimeUtil
							.convertToLocalDateTime(migrationBatchTriggerInformation.getBatchStartDateTime());

					newBatchEndDateTime = MigrationDateTimeUtil
							.convertToLocalDateTime(migrationBatchTriggerInformation.getBatchStartDateTime())
							.plusHours(migrationBatchTriggerInformation.getNumberOfHours());
				}

			}
		} else if (null != migrationBatchTriggerInformation.getNumberOfHours()
				&& migrationBatchTriggerInformation.getNumberOfHours() > -1) {

			String lastBatchParameters = scheduledBatchLogResponse.getBatchStartParameters();

			BatchStartParams startParams;
			try {
				startParams = objectMapper.readValue(lastBatchParameters, BatchStartParams.class);
			} catch (IOException e) {

				log.error(
						"JSON Mapping error occured in converting to BatchStartParams for string {} in calculateBatchTriggerParameters",
						lastBatchParameters);
				throw new JSONConversionException(
						"JSON Mapping error occured in converting to BatchStartParams in calculateBatchTriggerParameters",
						e.getCause());
			}

			newBatchStartDateTime = MigrationDateTimeUtil.convertToLocalDateTime(startParams.getEndDateTime())
					.plusSeconds(1);

			newBatchEndDateTime = MigrationDateTimeUtil.convertToLocalDateTime(startParams.getEndDateTime())
					.plusHours(migrationBatchTriggerInformation.getNumberOfHours());
		}

		BatchStartParams batchStartParams = new BatchStartParams();
		batchStartParams.setBeginDateTime(MigrationDateTimeUtil.convertToString(newBatchStartDateTime));
		batchStartParams.setEndDateTime(MigrationDateTimeUtil.convertToString(newBatchEndDateTime));
		batchStartParams.setTotalRecordsPerProcess(migrationBatchTriggerInformation.getNumberOfRecordsPerThread());

		return batchStartParams;

	}

	public String createBatchJob(String batchType, BatchStartParams batchStartParams, BigInteger totalRecords) {

		ScheduledBatchLogRequest scheduledBatchLogRequest = new ScheduledBatchLogRequest();

		scheduledBatchLogRequest.setBatchType(batchType);
		try {
			scheduledBatchLogRequest.setBatchStartParameters(objectMapper.writeValueAsString(batchStartParams));
		} catch (JsonProcessingException e) {

			log.error(
					"JSON Mapping error occured in converting to BatchStartParams string for object {} in createBatchJob",
					batchStartParams);
			throw new JSONConversionException(
					"JSON Mapping error occured in converting to BatchStartParams in createBatchJob", e.getCause());
		}
		scheduledBatchLogRequest.setTotalRecords(totalRecords);

		return coreScheduledBatchLogClient.saveBatch(scheduledBatchLogRequest).getBody().getBatchId();
	}

	public void sendSignedDraftIdsForProcessing(List<BigInteger> signedDraftIds, BatchStartParams batchStartParams,
			String batchId) {

		final int totalRecordsPerProcess = batchStartParams.getTotalRecordsPerProcess();

		// Below code partitions all signedDraftIds into chunk of TotalRecordsPerProcess
		final AtomicInteger counter = new AtomicInteger(0);
		final Collection<List<BigInteger>> partitionedColl = signedDraftIds.stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / totalRecordsPerProcess)).values();

		// Below code create chunks of partitionedColl to be scheduled at the same time
		// for sending
		final AtomicInteger messagesPerGroupCounter = new AtomicInteger(0);
		final Collection<List<List<BigInteger>>> messagesPerGroupColl = partitionedColl.stream()
				.collect(Collectors.groupingBy(it -> messagesPerGroupCounter.getAndIncrement() / messagesPerGroup))
				.values();

		final AtomicInteger counterInner = new AtomicInteger(0);

		messagesPerGroupColl.forEach(messagesPerGroup -> {

			messagesPerGroup.forEach(signedDraftChunk -> {

				log.info(
						"Inner Counter check in sendSignedDraftIdsForProcessing(), number of milliseconds added is {} and next Chunksize is {}",
						counterInner.get(), signedDraftChunk.size());

				String processId = createConcurrentProcess(signedDraftChunk.size(), batchId);

				ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition = new ConcurrentProcessMessageDefinition();

				concurrentProcessMessageDefinition.setBatchId(batchId);
				concurrentProcessMessageDefinition.setProcessId(processId);
				concurrentProcessMessageDefinition.setSignedDraftIds(signedDraftChunk);

				RunOnceTrigger runOnceTrigger = new RunOnceTrigger(counterInner.get());
				threadPoolTaskScheduler.schedule(new SendToProcessStartService(processStartQueueName, rabbitTemplate,
						concurrentProcessMessageDefinition), runOnceTrigger);
			});

			log.info("Counter is reset and more delay of {} milliseconds are added", messagesSendDelay);
			counterInner.addAndGet(messagesSendDelay);
		});

	}

	private String createConcurrentProcess(Integer batchSize, String batchId) {

		ConcurrentProcessLogDefinition concurrentProcessLogDefinition = new ConcurrentProcessLogDefinition();
		concurrentProcessLogDefinition.setBatchId(batchId);
		concurrentProcessLogDefinition.setProcessStatus(ProcessStatus.INPROGRESS.toString());
		concurrentProcessLogDefinition.setTotalRecordsInProcess(BigInteger.valueOf(batchSize));

		String processId = coreConcurrentProcessLogClient.saveConcurrentProcess(concurrentProcessLogDefinition)
				.getBody().getProcessId();
		return processId;
	}

}