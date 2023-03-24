package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class JSONConversionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3434712540996918191L;

	public JSONConversionException(String message) {
		super(message);
	}

	public JSONConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}