package com.ds.migration.coredata.model;

import java.math.BigInteger;
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
@Table(name = "coreconcurrentprocesslog")
public class CoreConcurrentProcessLog extends AuditData {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4628517513041385132L;

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "processid")
	UUID processId;

	@Column(name = "batchid")
	UUID batchId;

	@Column(name = "processstartdatetime")
	LocalDateTime processStartDateTime;

	@Column(name = "processenddatetime")
	LocalDateTime processEndDateTime;

	@Column(name = "processstatus")
	String processStatus;

	@Column(name = "totalrecordsinprocess")
	BigInteger totalRecordsInProcess;
}