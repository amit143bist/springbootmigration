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
@Table(name = "coreprocessfailurelog")
public class CoreProcessFailureLog extends AuditData {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2729284565481864993L;

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "processfailureid")
	UUID processFailureId;

	@Column(name = "processid")
	UUID processId;

	@Column(name = "failurecode")
	String failureCode;

	@Column(name = "failurereason")
	String failureReason;

	@Column(name = "failuredatetime")
	LocalDateTime failureDateTime;

	@Column(name = "successdatetime")
	LocalDateTime successDateTime;

	@Column(name = "failurerecordid")
	String failureRecordId;

	@Column(name = "failurestep")
	String failureStep;

	@Column(name = "retrystatus")
	String retryStatus;

	@Column(name = "retrycount")
	BigInteger retryCount;
}