package com.ds.migration.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class MissingSignatureDetailsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3330089138654965021L;

	public MissingSignatureDetailsException(String message) {
		super(message);
	}

	public MissingSignatureDetailsException(String message, Throwable cause) {
		super(message, cause);
	}
}