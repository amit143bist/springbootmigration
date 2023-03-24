package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EnvelopeNotCreatedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3330089138654965021L;

	public EnvelopeNotCreatedException(String message) {
		super(message);
	}

	public EnvelopeNotCreatedException(String message, Throwable cause) {
		super(message, cause);
	}
}