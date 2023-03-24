package com.ds.migration.feign.website.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "default", "accesstoken", "documentid", "documentsourcetype", "documentstatus", "documenttitle",
		"signatureauth", "signaturecerttype", "signatureemail", "signatureid", "signatureipaddress",
		"signaturesignername", "signaturestatus", "tsdocumentcomplete", "tsdocumentcreate", "tssignaturedocretrieval",
		"tssignaturesigned" })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureInformationResponse implements MigrationInformation {

	@JsonProperty("default")
	private String _default;
	@JsonProperty("accesstoken")
	private String accesstoken;
	@JsonProperty("documentid")
	private Integer documentid;
	@JsonProperty("documentsourcetype")
	private String documentsourcetype;
	@JsonProperty("documentstatus")
	private String documentstatus;
	@JsonProperty("documenttitle")
	private String documenttitle;
	@JsonProperty("signatureauth")
	private String signatureauth;
	@JsonProperty("signaturecerttype")
	private String signaturecerttype;
	@JsonProperty("signatureemail")
	private String signatureemail;
	@JsonProperty("signatureid")
	private Integer signatureid;
	@JsonProperty("signatureipaddress")
	private String signatureipaddress;
	@JsonProperty("signaturesignername")
	private String signaturesignername;
	@JsonProperty("signaturestatus")
	private String signaturestatus;
	@JsonProperty("tsdocumentcomplete")
	private String tsdocumentcomplete;
	@JsonProperty("tsdocumentcreate")
	private String tsdocumentcreate;
	@JsonProperty("tssignaturedocretrieval")
	private String tssignaturedocretrieval;
	@JsonProperty("tssignaturesigned")
	private String tssignaturesigned;

}