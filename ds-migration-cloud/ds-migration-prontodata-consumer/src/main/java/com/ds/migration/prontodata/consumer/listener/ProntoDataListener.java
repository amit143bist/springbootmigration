package com.ds.migration.prontodata.consumer.listener;

import java.math.BigInteger;
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
import com.ds.migration.common.exception.ListenerProcessingException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformation;
import com.ds.migration.feign.auditdata.domain.MigrationRecordIdInformationDefinition;
import com.ds.migration.feign.listener.AbstractMigrationListener;
import com.ds.migration.prontodata.consumer.client.MigrationProntoDataClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProntoDataListener extends AbstractMigrationListener<MigrationRecordIdInformationDefinition> {

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("${migration.queue.failure.retrylimit}")
	private long retryLimit;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	MigrationProntoDataClient migrationProntoDataClient;

	@RabbitListener(queues = "${migration.queue.name}")
	public void processMessage(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition,
			@Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

		log.info("MigrationRecordIdInformation received in processMessage() -> {} and xDeath value is {}",
				migrationRecordIdInformationDefinition, xDeath);

		super.processMessage(xDeath, retryLimit, migrationRecordIdInformationDefinition);

	}

	@Override
	public void callService(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition) {

		log.info("Calling updateSignedDraftWithDocuSignId for processId -> {}, totalRecords are {}",
				migrationRecordIdInformationDefinition.getProcessId(),
				migrationRecordIdInformationDefinition.getTotalRecords());

		try {

			MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinitionResp = migrationProntoDataClient
					.updateAllSignedDraftWithDocuSignId(migrationRecordIdInformationDefinition).getBody();

			List<MigrationRecordIdInformation> failedMigrationAuditDataRequestList = migrationRecordIdInformationDefinitionResp
					.getMigrationRecordIdInformationList();

			if (null != failedMigrationAuditDataRequestList && !failedMigrationAuditDataRequestList.isEmpty()) {

				processFailedMessage(migrationRecordIdInformationDefinition, failedMigrationAuditDataRequestList);
			}
		} catch (Exception exp) {

			log.error("Exception {} occurred with message {} for processId {} in callService", exp.getCause(),
					exp.getMessage(), migrationRecordIdInformationDefinition.getProcessId());
			processFailedMessage(migrationRecordIdInformationDefinition,
					migrationRecordIdInformationDefinition.getMigrationRecordIdInformationList());
		}

	}

	private void processFailedMessage(MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinition,
			List<MigrationRecordIdInformation> failedMigrationAuditDataRequestList) {

		MigrationRecordIdInformationDefinition migrationRecordIdInformationDefinitionFailure = new MigrationRecordIdInformationDefinition();
		migrationRecordIdInformationDefinitionFailure
				.setProcessId(migrationRecordIdInformationDefinition.getProcessId());
		migrationRecordIdInformationDefinitionFailure
				.setMigrationRecordIdInformationList(failedMigrationAuditDataRequestList);
		migrationRecordIdInformationDefinitionFailure
				.setTotalRecords(BigInteger.valueOf(failedMigrationAuditDataRequestList.size()));

		sendToDeadQueue(migrationRecordIdInformationDefinitionFailure, "", "");

		throw new ListenerProcessingException("Data Save Error in processFailedMessage for processId "
				+ migrationRecordIdInformationDefinition.getProcessId());
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
				"{} is thrown and exception message is {} in processing processId {}, retryCount is {}, retryLimit is {}, and errorStatusCode is {}",
				exp.getCause(), exp.getMessage(), migrationRecordIdInformationDefinition.getProcessId(), retryCount,
				retryLimit, httpStatus);
	}

}