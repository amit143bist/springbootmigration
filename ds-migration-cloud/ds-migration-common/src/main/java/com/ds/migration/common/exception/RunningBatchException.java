package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class RunningBatchException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3330089138654965021L;

	public RunningBatchException(String message) {
		super(message);
	}

	public RunningBatchException(String message, Throwable cause) {
		super(message, cause);
	}
}