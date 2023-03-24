package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class PendingQueueMessagesException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3330089138654965021L;

	public PendingQueueMessagesException(String message) {
		super(message);
	}

	public PendingQueueMessagesException(String message, Throwable cause) {
		super(message, cause);
	}
}