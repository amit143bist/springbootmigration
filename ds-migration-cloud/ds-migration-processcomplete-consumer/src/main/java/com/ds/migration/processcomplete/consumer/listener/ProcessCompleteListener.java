package com.ds.migration.processcomplete.consumer.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessLogDefinition;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import com.ds.migration.processcomplete.consumer.client.CoreConcurrentProcessLogClient;
import com.ds.migration.processcomplete.consumer.client.CoreScheduledBatchLogClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessCompleteListener extends AbstractMigrationListener<ConcurrentProcessLogDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	CoreScheduledBatchLogClient coreScheduledBatchLogClient;

	@Autowired
	CoreConcurrentProcessLogClient coreConcurrentProcessLogClient;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(ConcurrentProcessLogDefinition concurrentProcessLogDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.info("ConcurrentProcessLogDefinition received in processMessage() -> {} and xDeath value is {}",
				concurrentProcessLogDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, concurrentProcessLogDefinition);

	}

	@Override
	protected void callService(ConcurrentProcessLogDefinition concurrentProcessLogDefinition) {

		log.info("Calling updateConcurrentProcess for processId -> {} and batchId {}",
				concurrentProcessLogDefinition.getProcessId(), concurrentProcessLogDefinition.getBatchId());

		coreConcurrentProcessLogClient.updateConcurrentProcess(concurrentProcessLogDefinition,
				concurrentProcessLogDefinition.getProcessId());

		ResponseEntity<Long> processCount = coreConcurrentProcessLogClient
				.countPendingConcurrentProcessInBatch(concurrentProcessLogDefinition.getBatchId());

		if (0 == processCount.getBody().intValue()) {

			coreScheduledBatchLogClient.updateBatch(concurrentProcessLogDefinition.getBatchId());
		}
	}

	@Override
	protected void sendToDeadQueue(ConcurrentProcessLogDefinition concurrentProcessLogDefinition, String httpStatus,
			String errorHeaderMessage) {

		log.error("message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {}", concurrentProcessLogDefinition,
				errorHeaderMessage);

		rabbitTemplate.convertAndSend(deadQueueName, concurrentProcessLogDefinition, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	protected void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			ConcurrentProcessLogDefinition concurrentProcessLogDefinition) {

		log.error(
				"{} is thrown and exception message is {} in processing processId {} and batchId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), concurrentProcessLogDefinition.getProcessId(),
				concurrentProcessLogDefinition.getBatchId(), retryCount, retryLimit, httpStatus);
	}

}