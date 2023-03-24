package com.ds.migration.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.ds.migration.common.exception.InvalidInputException;

public class MigrationDateTimeUtil {

	public static String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	public static LocalDateTime convertToLocalDateTime(String dateTimeAsString) {

		return Optional.ofNullable(dateTimeAsString).map(str -> {

			return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
		}).orElseThrow(() -> new InvalidInputException(dateTimeAsString + " is null"));
	}

	public static String convertToString(LocalDateTime dateTime) {

		if (null != dateTime) {
			return dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
		}

		return null;
	}

	public static boolean isValidDateTime(String dateTimeAsString) {

		MigrationDateTimeUtil.convertToLocalDateTime(dateTimeAsString);

		return true;
	}
}