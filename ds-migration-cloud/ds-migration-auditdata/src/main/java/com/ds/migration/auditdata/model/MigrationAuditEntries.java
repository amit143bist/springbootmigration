package com.ds.migration.auditdata.model;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "migrationauditentries")
public class MigrationAuditEntries extends AuditData {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6789776495158587511L;

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "auditid")
	UUID auditId;

	@Column(name = "processid")
	UUID processId;

	@Column(name = "recordid")
	String recordId;

	@Column(name = "auditentrydatetime")
	LocalDateTime auditEntryDateTime;

	@Column(name = "recordphasestatus")
	String recordPhaseStatus;

	@Column(name = "recordphase")
	String recordPhase;

	@Column(name = "hashedentry")
	String hashedEntry;
}