package com.ds.migration.auditdata.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.migration.auditdata.model.MigrationAuditEntries;

@Repository(value = "migrationAuditDataRepository")
public interface MigrationAuditDataRepository extends CrudRepository<MigrationAuditEntries, UUID> {

	List<MigrationAuditEntries> findAllByRecordId(String recordId);

	// Not Used
	@Procedure(value = "insert_into_migrationauditentries")
	public Long createMigrationAuditData(@Param("_auditid") UUID _auditid, @Param("_recordid") String _recordid,
			@Param("_processid") UUID _processid, @Param("_auditentrydatetime") LocalDateTime _auditentrydatetime,
			@Param("_recordphasestatus") String _recordphasestatus, @Param("_recordphase") String _recordphase,
			@Param("_hashedentry") String _hashedentry);

}