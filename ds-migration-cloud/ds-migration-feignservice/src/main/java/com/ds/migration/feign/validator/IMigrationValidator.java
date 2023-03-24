package com.ds.migration.feign.validator;

import com.ds.migration.feign.domain.MigrationInformation;

public interface IMigrationValidator<T extends MigrationInformation> {

	void validateSaveData(T migrationInformation);
}