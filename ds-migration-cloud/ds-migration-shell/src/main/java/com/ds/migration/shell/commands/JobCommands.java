package com.ds.migration.shell.commands;

import java.util.Arrays;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.ds.migration.common.constant.BatchType;
import com.ds.migration.common.exception.InvalidInputException;
import com.ds.migration.common.exception.PendingQueueMessagesException;
import com.ds.migration.common.util.MigrationDateTimeUtil;
import com.ds.migration.shell.service.SendToBatchTriggerService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@ShellComponent
@Slf4j
public class JobCommands {

	@Autowired
	SendToBatchTriggerService batchTriggerService;

	@Value("${migration.validatecount.queues}")
	private String validationQueueNames;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@ShellMethod(value = "Migrate data from Pronto to DocuSign, validCommand is -> migrateProntoData batchStartDateTime $batchStartDateTime batchEndDateTime $batchEndDateTime numberOfHours $numberOfHours numberOfRecordsPerThread $numberOfRecordsPerThread", key = "migrateProntoData")
	public void migrateProntoData(
			@ShellOption(value = "batchStartDateTime", defaultValue = "defaultValue") String batchStartDateTime,
			@ShellOption(value = "batchEndDateTime", defaultValue = "defaultValue") String batchEndDateTime,
			@ShellOption(value = "numberOfHours", defaultValue = "-1") Integer numberOfHours,
			@ShellOption(value = "numberOfRecordsPerThread", defaultValue = "200") Integer numberOfRecordsPerThread)
			throws JsonProcessingException {

		log.info(
				"Inputs to JobCommands.migrateProntoData() are batchStartDateTime-> {} batchEndDateTime-> {} numberOfHours -> {} numberOfRecordsPerThread -> {}",
				batchStartDateTime, batchEndDateTime, numberOfHours, numberOfRecordsPerThread);

		List<String> queueNameList = Arrays.asList(validationQueueNames.split("\\s*,\\s*"));

		boolean holdNewRun = false;
		for (String queueName : queueNameList) {

			Integer messageCount = Integer
					.parseInt(rabbitAdmin.getQueueProperties(queueName).get("QUEUE_MESSAGE_COUNT").toString());

			if (messageCount > 0) {

				holdNewRun = true;
				log.error("{} messages are pending in queue -> {}", messageCount, queueName);
			}
		}

		if (holdNewRun) {

			throw new PendingQueueMessagesException(
					"Messages are pending to be processed in queue, please check logs for more details");
		} else {
			log.info("All queues are empty, so proceed to create new batch");
		}

		if (!"defaultValue".equalsIgnoreCase(batchStartDateTime)) {

			MigrationDateTimeUtil.isValidDateTime(batchStartDateTime);
			if (!"defaultValue".equalsIgnoreCase(batchEndDateTime)) {

				MigrationDateTimeUtil.isValidDateTime(batchEndDateTime);
				batchTriggerService.convertAndTriggerBatch(BatchType.PRONTOMIGRATION.toString(), batchStartDateTime,
						batchEndDateTime, numberOfRecordsPerThread);
			} else {

				if (numberOfHours == -1) {
					throw new InvalidInputException(
							"Input is wrong to trigger this batch, numberOfHours cannot be -1 with current command");
				}
				batchTriggerService.convertAndTriggerBatch(BatchType.PRONTOMIGRATION.toString(), batchStartDateTime,
						numberOfHours, numberOfRecordsPerThread);
			}
		} else if (numberOfHours > -1) {
			batchTriggerService.convertAndTriggerBatch(BatchType.PRONTOMIGRATION.toString(), numberOfHours,
					numberOfRecordsPerThread);
		} else {
			log.info(
					"ValidCommand sample is -> migrateProntoData batchStartDateTime $batchStartDateTime batchEndDateTime $batchEndDateTime numberOfHours $numberOfHours numberOfRecordsPerThread $numberOfRecordsPerThread");

			log.error("Inputs are wrong to trigger this batch");
			throw new InvalidInputException("Inputs are wrong to trigger this batch");
		}

		log.info(
				"*********************************Message successfully delivered to the queue*********************************");
		log.info("*********************************Shutting down this service*********************************");
	}
}