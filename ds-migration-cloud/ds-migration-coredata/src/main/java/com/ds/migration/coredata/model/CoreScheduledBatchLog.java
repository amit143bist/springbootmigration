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
@Table(name = "corescheduledbatchlog")
public class CoreScheduledBatchLog extends AuditData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6231169563307669261L;

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "batchid")
	UUID batchId;

	@Column(name = "batchtype")
	String batchType;

	@Column(name = "batchstartdatetime")
	LocalDateTime batchStartDateTime;

	@Column(name = "batchenddatetime")
	LocalDateTime batchEndDateTime;

	@Column(name = "batchstartparameters")
	String batchStartParameters;

	@Column(name = "totalrecords")
	BigInteger totalRecords;
}