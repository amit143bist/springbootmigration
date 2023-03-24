package com.ds.migration.batchtrigger.consumer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.ds.migration.feign.batch.domain.ConcurrentProcessMessageDefinition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SendToProcessStartService implements Runnable {

	private String processStartQueueName;

	private RabbitTemplate rabbitTemplate;

	private ConcurrentProcessMessageDefinition concurrentProcessMessageDefinition;

	public void run() {

		log.info(
				"************************* Sending message in sendConcurrentProcessToQueue() to queue -> {} for processId -> {} *************************",
				processStartQueueName, concurrentProcessMessageDefinition.getProcessId());
		rabbitTemplate.convertAndSend(processStartQueueName, concurrentProcessMessageDefinition);

	}
}