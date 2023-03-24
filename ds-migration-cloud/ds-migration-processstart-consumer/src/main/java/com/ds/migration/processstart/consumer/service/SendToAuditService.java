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

import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToAuditService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.auditqueue.name}")
	private String migrationAuditQueueName;

	@Value("${migration.application.totalrecordsperqueuemessage}")
	private Integer totalRecordsPerQueueMessage;

	public void sendAuditMessage(List<MigrationAuditDataRequest> migrationAuditDataRequestList, String processId) {

		MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
		migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestList);
		migrationAuditDataDefinition.setTotalRecords(BigInteger.valueOf(migrationAuditDataRequestList.size()));
		migrationAuditDataDefinition.setProcessId(processId);

		log.info(
				"************************* Sending message in sendAuditMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
				migrationAuditQueueName, processId, migrationAuditDataRequestList.get(0).getRecordId());
		rabbitTemplate.convertAndSend(migrationAuditQueueName, migrationAuditDataDefinition);

	}

	public void sendSplitAuditMessage(List<MigrationAuditDataRequest> migrationAuditDataRequestList, String processId) {

		log.info("Splitting big migrationAuditDataRequestList of size -> {} into chunk of small batches of size -> {}",
				migrationAuditDataRequestList.size(), totalRecordsPerQueueMessage);

		final AtomicInteger counter = new AtomicInteger(0);
		final Collection<List<MigrationAuditDataRequest>> partitionedColl = migrationAuditDataRequestList.stream()
				.collect(Collectors.groupingBy(it -> counter.getAndIncrement() / totalRecordsPerQueueMessage)).values();

		partitionedColl.forEach(migrationAuditDataRequestChunk -> {

			MigrationAuditDataDefinition migrationAuditDataDefinition = new MigrationAuditDataDefinition();
			migrationAuditDataDefinition.setMigrationAuditDataRequestList(migrationAuditDataRequestChunk);
			migrationAuditDataDefinition.setTotalRecords(BigInteger.valueOf(migrationAuditDataRequestChunk.size()));
			migrationAuditDataDefinition.setProcessId(processId);

			log.info(
					"************************* Sending message in sendAuditMessage() to queue -> {} for processId -> {}, firstRecordId -> {} *************************",
					migrationAuditQueueName, processId, migrationAuditDataRequestChunk.get(0).getRecordId());
			rabbitTemplate.convertAndSend(migrationAuditQueueName, migrationAuditDataDefinition);
		});

	}
}