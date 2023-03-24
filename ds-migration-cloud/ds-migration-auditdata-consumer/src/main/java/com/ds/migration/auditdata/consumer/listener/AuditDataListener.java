package com.ds.migration.auditdata.consumer.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.ds.migration.auditdata.consumer.client.MigrationAuditDataClient;
import com.ds.migration.common.constant.MQMessageProperties;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataDefinition;
import com.ds.migration.feign.auditdata.domain.MigrationAuditDataRequest;
import com.ds.migration.feign.listener.AbstractMigrationListener;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditDataListener extends AbstractMigrationListener<MigrationAuditDataDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	MigrationAuditDataClient migrationAuditDataClient;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(MigrationAuditDataDefinition migrationAuditDataDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.debug("MigrationAuditDataRequest received in processMessage() -> {} and xDeath value is {}",
				migrationAuditDataDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, migrationAuditDataDefinition);

	}

	@Override
	public void callService(MigrationAuditDataDefinition migrationAuditDataDefinition) {

		log.info(
				"Calling migrationAuditDataClient.saveAllAuditDataProcedure for processId -> {}, totalRecords are {} and firstRecordId is {}",
				migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
				migrationAuditDataDefinition.getMigrationAuditDataRequestList().get(0).getRecordId());

		migrationAuditDataClient.saveAllAuditData(migrationAuditDataDefinition);

		log.info(
				"Completed migrationAuditDataClient.saveAllAuditDataProcedure for processId -> {}, totalRecords are {} and firstRecordId is {}",
				migrationAuditDataDefinition.getProcessId(), migrationAuditDataDefinition.getTotalRecords(),
				migrationAuditDataDefinition.getMigrationAuditDataRequestList().get(0).getRecordId());

	}

	@Override
	public void sendToDeadQueue(MigrationAuditDataDefinition migrationAuditDataDefinition, String httpStatus,
			String errorHeaderMessage) {

		log.error("message in sendToDeadQueue() is -> {}, and errorHeaderMessage is {}", migrationAuditDataDefinition,
				errorHeaderMessage);

		rabbitTemplate.convertAndSend(deadQueueName, migrationAuditDataDefinition, m -> {
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORSTATUSCODE.toString(), httpStatus);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORREASON.toString(), errorHeaderMessage);
			m.getMessageProperties().getHeaders().put(MQMessageProperties.ERRORTIMESTAMP.toString(),
					MigrationDateTimeUtil.convertToString(LocalDateTime.now()));
			return m;
		});
	}

	@Override
	public void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			MigrationAuditDataDefinition migrationAuditDataDefinition) {

		MigrationAuditDataRequest migrationAuditDataRequest = migrationAuditDataDefinition
				.getMigrationAuditDataRequestList().get(0);

		log.error(
				"{} is thrown and exception message is {} in processing failureRecordId {} and processId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), migrationAuditDataRequest.getRecordId(),
				migrationAuditDataRequest.getProcessId(), retryCount, retryLimit, httpStatus);
	}

}