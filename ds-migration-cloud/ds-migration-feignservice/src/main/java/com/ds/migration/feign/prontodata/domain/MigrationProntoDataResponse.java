package com.ds.migration.feign.prontodata.domain;

import com.ds.migration.feign.domain.MigrationInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "signeddraftid", "documentid", "documenttitle", "tsdocumentcreate", "tsdocumentcomplete",
		"documentstatus", "documentsourcetype", "signatureid", "signaturecerttype", "signaturesignername",
		"signatureipaddress", "signatureemail", "tssignaturedocretrieval", "tssignaturesigned", "signaturestatus",
		"signatureauth", "accesstoken" })
public class MigrationProntoDataResponse implements MigrationInformation {

	@JsonProperty("signeddraftid")
	private Integer signeddraftid;
	@JsonProperty("documentid")
	private Integer documentid;
	@JsonProperty("documenttitle")
	private String documenttitle;
	@JsonProperty("tsdocumentcreate")
	private String tsdocumentcreate;
	@JsonProperty("tsdocumentcomplete")
	private String tsdocumentcomplete;
	@JsonProperty("documentstatus")
	private String documentstatus;
	@JsonProperty("documentsourcetype")
	private String documentsourcetype;
	@JsonProperty("signatureid")
	private Integer signatureid;
	@JsonProperty("signaturecerttype")
	private String signaturecerttype;
	@JsonProperty("signaturesignername")
	private String signaturesignername;
	@JsonProperty("signatureipaddress")
	private String signatureipaddress;
	@JsonProperty("signatureemail")
	private String signatureemail;
	@JsonProperty("tssignaturedocretrieval")
	private String tssignaturedocretrieval;
	@JsonProperty("tssignaturesigned")
	private String tssignaturesigned;
	@JsonProperty("signaturestatus")
	private String signaturestatus;
	@JsonProperty("signatureauth")
	private String signatureauth;
	@JsonProperty("accesstoken")
	private String accesstoken;

}