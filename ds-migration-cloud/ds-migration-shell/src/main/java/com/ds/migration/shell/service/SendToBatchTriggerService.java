package com.ds.migration.shell.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.feign.batch.domain.MigrationBatchTriggerInformation;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToBatchTriggerService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.queue.name}")
	private String migrationQueueName;

	public void convertAndTriggerBatch(String batchType, String batchStartDateTime, String batchEndDateTime,
			Integer numberOfRecordsPerThread) throws JsonProcessingException {

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				batchType, batchStartDateTime, batchEndDateTime, null, numberOfRecordsPerThread);
		sendBatchMessage(migrationBatchTriggerInformation);

	}

	public void convertAndTriggerBatch(String batchType, Integer numberOfHours, Integer numberOfRecordsPerThread)
			throws JsonProcessingException {

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				batchType, null, null, numberOfHours, numberOfRecordsPerThread);
		sendBatchMessage(migrationBatchTriggerInformation);

	}

	public void convertAndTriggerBatch(String batchType, String batchStartDateTime, Integer numberOfHours,
			Integer numberOfRecordsPerThread) throws JsonProcessingException {

		MigrationBatchTriggerInformation migrationBatchTriggerInformation = new MigrationBatchTriggerInformation(
				batchType, batchStartDateTime, null, numberOfHours, numberOfRecordsPerThread);
		sendBatchMessage(migrationBatchTriggerInformation);

	}

	private void sendBatchMessage(MigrationBatchTriggerInformation migrationBatchTriggerInformation)
			throws JsonProcessingException {

		log.info(
				"************************* Sending message in convertAndTriggerBatch() to queue -> {} for batchType -> {} *************************",
				migrationQueueName, migrationBatchTriggerInformation.getBatchType());
		rabbitTemplate.convertAndSend(migrationQueueName, migrationBatchTriggerInformation);
	}
}