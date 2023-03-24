package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
public class ResourceConditionFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3330089138654965021L;

	public ResourceConditionFailedException(String message) {
		super(message);
	}

	public ResourceConditionFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}