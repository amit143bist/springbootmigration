package com.ds.migration.website.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "signatureId", "documentId", "envelopeId" })
public class DocumentDetails {

	@JsonProperty("signatureId")
	public Long signatureId;
	@JsonProperty("documentId")
	public Long documentId;
	@JsonProperty("envelopeId")
	public String envelopeId;

}