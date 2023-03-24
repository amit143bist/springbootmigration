package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ListenerProcessingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3434712540996918191L;

	public ListenerProcessingException(String message) {
		super(message);
	}

	public ListenerProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

}