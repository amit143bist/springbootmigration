package com.ds.migration.processstart.consumer.service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToSaveDataService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.prontoqueue.name}")
	private String migrationProntoQueueName;

	@Value("${migration.recordqueue.name}")
	private String migrationRecordQueueName;

	@Value("${migration.application.totalrecordsperqueuemessage}")
	private Integer totalRecordsPerQueueMessage;

	public void sendSaveDataMessage(List<MigrationRecordIdInformation> migrationRecordIdInformationList,
			String processId) {

		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinition
				.setTotalRecords(BigInteger.valueOf(migrationRecordIdInformationList.size()));
		migrationRecordIdInformationDefinition.setProcessId(processId);
		migrationRecordIdInformationDefinition.setMigrationRecordIdInformationList(migrationRecordIdInformationList);

		log.info(
				"************************* Sending message in sendSaveDataMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
				migrationProntoQueueName, processId, migrationRecordIdInformationList.get(0).getRecordId());
		rabbitTemplate.convertAndSend(migrationProntoQueueName, migrationRecordIdInformationDefinition);

		log.info(
				"************************* Sending message in sendSaveDataMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
				migrationRecordQueueName, processId, migrationRecordIdInformationList.get(0).getRecordId());
		rabbitTemplate.convertAndSend(migrationRecordQueueName, migrationRecordIdInformationDefinition);

	}

	public void sendSplitSaveDataMessage(List<MigrationRecordIdInformation> migrationRecordIdInformationList,
			String processId) {

		log.info(
				"Splitting big migrationRecordIdInformationList of size -> {} into chunk of small batches of size -> {}",
				migrationRecordIdInformationList.size(), totalRecordsPerQueueMessage);

		final AtomicInteger counter = new AtomicInteger(0);
		final Collection<List<MigrationRecordIdInformation>> partitionedColl = migrationRecordIdInformationList.stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / totalRecordsPerQueueMessage)).values();

		partitionedColl.forEach(migrationRecordIdInformationChunk -> {

			MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition = new MigrationRecordIdInformationDefinition();
			migrationRecordIdInformationDefinition
					.setTotalRecords(BigInteger.valueOf(migrationRecordIdInformationChunk.size()));
			migrationRecordIdInformationDefinition.setProcessId(processId);
			migrationRecordIdInformationDefinition
					.setMigrationRecordIdInformationList(migrationRecordIdInformationChunk);

			log.info(
					"************************* Sending message in sendSaveDataMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
					migrationProntoQueueName, processId, migrationRecordIdInformationChunk.get(0).getRecordId());
			rabbitTemplate.convertAndSend(migrationProntoQueueName, migrationRecordIdInformationDefinition);

			log.info(
					"************************* Sending message in sendSaveDataMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
					migrationRecordQueueName, processId, migrationRecordIdInformationChunk.get(0).getRecordId());
			rabbitTemplate.convertAndSend(migrationRecordQueueName, migrationRecordIdInformationDefinition);
		});

	}
}