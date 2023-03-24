package com.ds.migration.processfailure.consumer.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.coredata.domain.ConcurrentProcessFailureLogDefinition;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import com.ds.migration.processfailure.consumer.client.CoreProcessFailureLogClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CoreProcessFailureLogListener extends AbstractMigrationListener<ConcurrentProcessFailureLogDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	CoreProcessFailureLogClient coreProcessFailureLogClient;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.info("ConcurrentProcessFailureLogDefinition received in processMessage() -> {} and xDeath value is {}",
				concurrentProcessFailureLogDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, concurrentProcessFailureLogDefinition);

	}

	@Override
	public void callService(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition) {

		log.info("Calling saveFailureLog/updateFailureLog for processId -> {}, failureRecordId are {}",
				concurrentProcessFailureLogDefinition.getProcessId(),
				concurrentProcessFailureLogDefinition.getProcessFailureId());

		if (StringUtils.isEmpty(concurrentProcessFailureLogDefinition.getProcessFailureId())) {

			coreProcessFailureLogClient.saveFailureLog(concurrentProcessFailureLogDefinition);
		} else {

			coreProcessFailureLogClient.updateFailureLog(concurrentProcessFailureLogDefinition,
					concurrentProcessFailureLogDefinition.getProcessFailureId());
		}
	}

	@Override
	public void sendToDeadQueue(ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition,
			String httpStatus, String errorHeaderMessage) {

		log.error("message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {}",
				concurrentProcessFailureLogDefinition, errorHeaderMessage);

		rabbitTemplate.convertAndSend(deadQueueName, concurrentProcessFailureLogDefinition, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	public void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			ConcurrentProcessFailureLogDefinition concurrentProcessFailureLogDefinition) {

		log.error(
				"{} is thrown and exception message is {} in processing failureRecordId {} and processId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), concurrentProcessFailureLogDefinition.getFailureRecordId(),
				concurrentProcessFailureLogDefinition.getProcessId(), retryCount, retryLimit, httpStatus);
	}

}