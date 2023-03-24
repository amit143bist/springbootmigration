package com.ds.migration.auditdata.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "migrationrecordidentries")
public class MigrationRecordIdEntries extends AuditData {

	/**
	 * 
	 */
	private static final long serialVersionUID = -742906917076259036L;

	@Id
	@Column(name = "recordid")
	String recordId;

	@Column(name = "docusignid")
	UUID docusignId;
}