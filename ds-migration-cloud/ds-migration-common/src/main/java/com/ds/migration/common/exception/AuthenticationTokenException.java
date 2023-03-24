package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AuthenticationTokenException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3434712540996918191L;

	public AuthenticationTokenException(String message) {
		super(message);
	}

	public AuthenticationTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}