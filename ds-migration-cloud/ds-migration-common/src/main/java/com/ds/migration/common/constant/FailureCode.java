package com.ds.migration.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FailureCode {

	ERROR_101("DocumentValidationFailureException"),
	ERROR_102("DocumentNotFoundException"),
	ERROR_103("EnvelopeNotCreatedException"),
	ERROR_104("ResponseStatusException"),
	ERROR_105("URLConnectionException"),
	ERROR_106("UnknownException"),
	ERROR_107("MissingSignatureDetailsException");

	@Getter
	private String failureCodeDescription;
}