package com.ds.migration.processstart.consumer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendToFailureService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Value("${migration.failurequeue.name}")
	private String migrationFailureQueueName;

	public void sendFailureMessage(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition) {

		log.info(
				"************************* Sending message in sendFailureMessage() to queue -> {} for failureRecordId -> {} in processId {} *************************",
				migrationFailureQueueName, concurrentProcessFailureLogDefinition.getFailureRecordId(),
				concurrentProcessFailureLogDefinition.getProcessId());
		rabbitTemplate.convertAndSend(migrationFailureQueueName, concurrentProcessFailureLogDefinition);
	}
}