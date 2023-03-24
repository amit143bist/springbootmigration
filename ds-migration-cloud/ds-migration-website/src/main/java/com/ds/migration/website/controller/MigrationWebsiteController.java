package com.ds.migration.website.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ds.migration.common.exception.DocumentNotFoundException;
import com.ds.migration.common.exception.EnvelopeNotCreatedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.ds.migration.feign.website.domain.SignatureInformationResponse;
import com.ds.migration.website.service.DocusignService;
import com.ds.migration.website.service.OracleIBISService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MigrationWebsiteController {

	@Autowired
	private OracleIBISService oracleIBISService;

	@Autowired
	private DocusignService docusignService;

	// https://cpm.texturacorp.com/prontosvr2/TXStatus.asp?DocID=11221222&SigID=1028350914&at=AEDB055DAE256D8C85209FC3C5B50CA088BF60AE
	@RequestMapping(value = "/TXStatus.asp", method = RequestMethod.GET)
	public RedirectView getSigningReport(@RequestParam(value = "SigID", required = true) Long signatureId,
			@RequestParam(value = "DocID", required = true) Long documentId,
			@RequestParam(value = "at", required = true) String accesstoken, RedirectAttributes attributes,
			HttpServletRequest request) {

		log.info("MigrationWebsiteController.getSigningReport() signatureId -> {} documentId -> {}", signatureId,
				documentId);
		RedirectView redirect;
		attributes.addFlashAttribute("signatureId", signatureId);
		attributes.addFlashAttribute("documentId", documentId);
		attributes.addFlashAttribute("accesstoken", accesstoken);

		try {
			String envelopeId = oracleIBISService.getEnvelopeByDocument(documentId, accesstoken);
			log.info("oracleIBISService: Document {} was  migrated, DocuSign EnvelopeID assigned is {}", documentId, envelopeId);
			attributes.addFlashAttribute("envelopeId", envelopeId);

			redirect = new RedirectView("renderAuditReport");
		} catch (EnvelopeNotCreatedException e){
			log.error("oracleIBISService: Document {} was not migrated, DocuSign EnvelopeID assigned is null", documentId);
			redirect = new RedirectView("renderRecordNoMigratedError");
		} catch (DocumentNotFoundException e) {
			log.error("GenericError encounter: Document -> {}, exception was {}", documentId, e.getMessage());
			redirect = new RedirectView("renderGenericError");
		}

		redirect.setHosts(new String[] { request.getHeader("X-FORWARDED-HOST") });

		return redirect;
	}

	@RequestMapping(value = "/renderAuditReport", method = RequestMethod.GET)
	public String renderAuditReport(Model model) {
		boolean modelIsEmpty = model.asMap().isEmpty();

		log.info("Inside renderAuditReport, Model is empty: {}", modelIsEmpty);
		if (!modelIsEmpty) {
			log.info("Inside renderAuditReport signatureId -> {} documentId -> {} envelopeId -> {} and accesstoken -> {}", model.asMap().get("signatureId"),
					model.asMap().get("documentId"), model.asMap().get("envelopeId"), model.asMap().get("accesstoken"));
		}

		if (model.asMap().isEmpty()) {
			log.info("Rendering renderGenericError");
			return "RefreshRenderError";
		} else {
			log.info("Rendering SigningReport");
			return "SigningReport";
		}

	}

	@RequestMapping(value = "/renderRecordNoMigratedError", method = RequestMethod.GET)
	public String renderRecordNoMigratedError(Model model) {

		boolean modelIsEmpty = model.asMap().isEmpty();
		log.info("Inside renderRecordNoMigratedError, Model is empty: {}", modelIsEmpty);
		if (!modelIsEmpty) {
			log.info("Inside renderRecordNoMigratedError signatureId -> {} documentId -> {} envelopeId -> {} and accesstoken -> {}", model.asMap().get("signatureId"),
					model.asMap().get("documentId"), model.asMap().get("envelopeId"), model.asMap().get("accesstoken"));
		}

		if (model.asMap().isEmpty()) {
			log.info("Rendering RefreshRenderError");
			return "RefreshRenderError";
		} else {
			log.info("Rendering RecordNoMigratedError");
			return "RecordNoMigratedError";
		}
	}

	@RequestMapping(value = "/renderGenericError", method = RequestMethod.GET)
	public String renderGenericError(Model model) {

		boolean modelIsEmpty = model.asMap().isEmpty();
		log.info("Inside renderGenericError, Model is empty: {}", modelIsEmpty);
		if (!modelIsEmpty) {
			log.info("Inside renderGenericError signatureId -> {} documentId -> {} envelopeId -> {} and accesstoken -> {}", model.asMap().get("signatureId"),
					model.asMap().get("documentId"), model.asMap().get("envelopeId"), model.asMap().get("accesstoken"));
		}

		if (model.asMap().isEmpty()) {
			log.info("Rendering RefreshRenderError");
			return "RefreshRenderError";
		} else {
			log.info("Rendering GenericError");
			return "GenericError";
		}
	}

	@RequestMapping(value = "/signatures", method = RequestMethod.POST)
	public @ResponseBody List<SignatureInformationResponse> getSignaturesDetails(
			@RequestBody DocumentDetails documentDetails) {

		log.info(
				"Calling MigrationWebsiteController.getSignatures for documentId -> {}, signature -> {} and envelopeId -> {}",
				documentDetails.getDocumentId(), documentDetails.getSignatureId(), documentDetails.getEnvelopeId());

		log.info("start time {}", LocalDateTime.now());
		List<SignatureInformationResponse> signatures = docusignService.getSignaturesInformation(
				documentDetails.getDocumentId(), documentDetails.getSignatureId(), documentDetails.getEnvelopeId());
		log.info("end time {}", LocalDateTime.now());

		log.debug("We have retrieved {} signatures", signatures.size());

		return signatures;

	}

	@RequestMapping(value = "/document", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<byte[]> getDocument(@RequestParam(value = "envelopeId", required = true) String envelopeId) {

		log.info("Calling MigrationWebsiteController.getDocument for envelopeId -> {}", envelopeId);

		byte[] document = docusignService.getDocument(envelopeId, 1L);

		log.debug(" We have retrieved document with size {} ", document.length);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "download.pdf" + "\"")
				.body(document);

	}
}