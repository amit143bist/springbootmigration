package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SEE_OTHER)
public class URLConnectionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3434712540996918191L;

	public URLConnectionException(String message) {
		super(message);
	}

	public URLConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}