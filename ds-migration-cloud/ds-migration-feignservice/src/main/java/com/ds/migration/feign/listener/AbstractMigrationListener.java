package com.ds.migration.feign.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.ds.migration.common.exception.InvalidInputException;
import com.ds.migration.common.exception.JSONConversionException;
import com.ds.migration.common.exception.ListenerProcessingException;
import com.ds.migration.common.exception.RunningBatchException;
import com.ds.migration.common.exception.URLConnectionException;
import com.ds.migration.feign.domain.MigrationInformation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMigrationListener<T extends MigrationInformation> {

	protected abstract void callService(T migrationInformation);

	protected abstract void sendToDeadQueue(T migrationInformation, String httpStatus, String errorHeaderMessage);

	protected abstract void logErrorMessage(long retryCount, Exception exp, String expReason, String httpStatus,
			T migrationInformation);

	protected void processMessage(List<Map<String, Object>> xDeath, long retryLimit, T migrationInformation) {

		long retryCount = 0;
		if (xDeath != null) {
			Optional<Long> count = xDeath.stream().flatMap(m -> m.entrySet().stream())
					.filter(e -> e.getKey().equals("count")).findFirst().map(e -> (Long) e.getValue());
			if (count.isPresent()) {
				retryCount = count.get().longValue();
				log.debug("RetryCount is -> {}", retryCount);
			}
		}

		try {

			callService(migrationInformation);
			log.debug("Succesfully completed the callService and processMessage for {}", migrationInformation);

		} catch (ListenerProcessingException exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
		} catch (InvalidInputException exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
			sendToDeadQueue(migrationInformation, "", exp.getMessage());
		} catch (RunningBatchException exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
			sendToDeadQueue(migrationInformation, "", exp.getMessage());
		} catch (JSONConversionException exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
			sendToDeadQueue(migrationInformation, "", exp.getMessage());
		} catch (HttpClientErrorException exp) {

			StringBuilder builder = new StringBuilder();
			builder.append(exp.getStatusCode());
			builder.append("_");
			builder.append(exp.getRawStatusCode());
			builder.append("_");
			builder.append(exp.getResponseBodyAsString());
			builder.append("_");
			builder.append(exp.getResponseHeaders());

			logErrorMessage(retryCount, exp, builder.toString(), "", migrationInformation);
			sendToDeadQueue(migrationInformation, "", exp.getMessage());

		} catch (ResponseStatusException exp) {

			logErrorMessage(retryCount, exp, exp.getReason(), null != exp.getStatus() ? exp.getStatus().toString() : "",
					migrationInformation);
			if (retryCount > retryLimit) {

				sendToDeadQueue(migrationInformation, null != exp.getStatus() ? exp.getStatus().toString() : "",
						exp.getReason());
			} else {

				log.error(
						"Since retryCount -> {} is not more than retryLimit -> {} so throwing ResponseStatusException -> {} again",
						retryCount, retryLimit, exp);
				throw exp;
			}
		} catch (URLConnectionException exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
			if (retryCount > retryLimit) {

				sendToDeadQueue(migrationInformation, "", exp.getMessage());
			} else {

				log.error(
						"Since retryCount -> {} is not more than retryLimit -> {} so throwing URLConnectionException -> {} again",
						retryCount, retryLimit, exp);
				throw exp;
			}
		} catch (Exception exp) {

			logErrorMessage(retryCount, exp, exp.getMessage(), "", migrationInformation);
			if (retryCount > retryLimit) {

				sendToDeadQueue(migrationInformation, "", exp.getMessage());
			} else {

				log.error(
						"Since retryCount -> {} is not more than retryLimit -> {} so throwing generic exception -> {} again",
						retryCount, retryLimit, exp);
				throw exp;
			}
		}
	}
}