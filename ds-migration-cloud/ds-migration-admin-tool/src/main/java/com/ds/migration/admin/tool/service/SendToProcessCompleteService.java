package com.ds.migration.admin.tool.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToProcessCompleteService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.processcompletequeue.name}")
	private String migrationProcessCompleteQueueName;

	public void sendProcessCompleteMessage(ConcurrentProcessLogDefinition concurrentProcessLogDefinition) {

		log.info(
				"************************* Sending message in sendProcessCompleteMessage() to queue -> {} for processId -> {}, and batchId -> {} *************************",
				migrationProcessCompleteQueueName, concurrentProcessLogDefinition.getProcessId(),
				concurrentProcessLogDefinition.getBatchId());
		
		rabbitTemplate.convertAndSend(migrationProcessCompleteQueueName, concurrentProcessLogDefinition);
	}
}