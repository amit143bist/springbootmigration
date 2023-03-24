package com.ds.migration.recorddata.consumer.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import com.ds.migration.recorddata.consumer.client.MigrationRecordIdClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MigrationDataListener extends AbstractMigrationListener<MigrationRecordIdInformationDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	MigrationRecordIdClient migrationRecordIdClient;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.debug("MigrationRecordIdInformation received in processMessage() -> {} and xDeath value is {}",
				migrationRecordIdInformationDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, migrationRecordIdInformationDefinition);

	}

	@Override
	public void callService(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition) {

		log.info(
				"Calling migrationRecordIdClient.saveAllRecordIdData for processId -> {}, totalRecords are {} and recordId is {}",
				migrationRecordIdInformationDefinition.getProcessId(),
				migrationRecordIdInformationDefinition.getTotalRecords(),
				migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().get(0).getRecordId());

		migrationRecordIdClient.saveAllRecordIdData(migrationRecordIdInformationDefinition);

		log.info(
				"Completed migrationRecordIdClient.saveAllRecordIdData for processId -> {}, totalRecords are {} and recordId is {}",
				migrationRecordIdInformationDefinition.getProcessId(),
				migrationRecordIdInformationDefinition.getTotalRecords(),
				migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList().get(0).getRecordId());

	}

	@Override
	public void sendToDeadQueue(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition,
			String httpStatus, String errorHeaderMessage) {

		log.error("message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {}",
				migrationRecordIdInformationDefinition, errorHeaderMessage);

		rabbitTemplate.convertAndSend(deadQueueName, migrationRecordIdInformationDefinition, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	public void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition) {

		log.error(
				"{} is thrown and exception message is {} in processing processId {}, totalFailedRecords -> {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), migrationRecordIdInformationDefinition.getProcessId(),
				migrationRecordIdInformationDefinition.getTotalRecords(), retryCount, retryLimit, httpStatus);
	}

}