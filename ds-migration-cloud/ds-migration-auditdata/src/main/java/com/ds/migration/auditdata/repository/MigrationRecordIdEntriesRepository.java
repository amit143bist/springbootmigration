package com.ds.migration.auditdata.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ds.migration.auditdata.model.MigrationRecordIdEntries;

@Repository(value = "migrationRecordIdEntriesRepository")
public interface MigrationRecordIdEntriesRepository extends CrudRepository<MigrationRecordIdEntries, String> {

}