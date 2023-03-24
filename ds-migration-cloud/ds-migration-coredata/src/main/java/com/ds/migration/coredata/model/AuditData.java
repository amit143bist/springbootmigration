package com.ds.migration.coredata.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "createddatetime", "updateddatetime", "createdby", "updatedby" }, allowGetters = true)
public abstract class AuditData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5957359283884761164L;

	@Column(name = "createddatetime", nullable = false, updatable = false)
	@CreatedDate
	LocalDateTime createdDateTime;

	@Column(name = "updateddatetime", insertable = false)
	@LastModifiedDate
	LocalDateTime updatedDateTime;

	@Column(name = "createdby", nullable = false, updatable = false)
	@CreatedBy
	String createdBy;

	@Column(name = "updatedby", insertable = false)
	@LastModifiedBy
	String updatedBy;
}