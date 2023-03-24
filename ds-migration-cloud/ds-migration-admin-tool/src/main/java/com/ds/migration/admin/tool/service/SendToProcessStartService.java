package com.ds.migration.admin.tool.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToProcessStartService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.processstartqueue.name}")
	private String migrationProcessStartQueueName;

	public void sendProcessStartMessage(ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition) {

		log.info(
				"************************* Sending message in sendProcessStartMessage() to queue -> {} for processId -> {}, failureIds {} and batchId -> {} *************************",
				migrationProcessStartQueueName, concurrentProcessMessageDefinition.getProcessId(),
				concurrentProcessMessageDefinition.getSignedDraftIds(),
				concurrentProcessMessageDefinition.getBatchId());

		rabbitTemplate.convertAndSend(migrationProcessStartQueueName, concurrentProcessMessageDefinition);
	}
}