package com.ds.migration.processstart.consumer.service;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ds.migration.common.constant.MigrationAppConstants;
import com.ds.migration.common.exception.URLConnectionException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RetrieveProntoDocumentService {

	@Value("${migration.prontoapplication.uri}")
	private String migrationProntoUri;

	@Autowired
	@Qualifier("prontoRestTemplate")
	RestTemplate prontoRestTemplate;

	public String verifyDocument(Integer docId, Integer sigId, String prontoAccessToken, String recordId,
			String processId) {

		StringBuilder prontoUrlBuilder = new StringBuilder();

		prontoUrlBuilder.append(migrationProntoUri);
		prontoUrlBuilder.append(MigrationAppConstants.PRONTO_QUERY_PARAM_1);
		prontoUrlBuilder.append(docId);
		prontoUrlBuilder.append(MigrationAppConstants.PRONTO_QUERY_PARAM_2);
		prontoUrlBuilder.append(sigId);
		prontoUrlBuilder.append(MigrationAppConstants.PRONTO_QUERY_PARAM_3);
		prontoUrlBuilder.append(prontoAccessToken);

		String prontoUrl = prontoUrlBuilder.toString();

		log.debug("Calling verifyDocument with ProntoURL -> {} for recordId ->{} in processId {}", prontoUrl, recordId,
				processId);

		try {

			Document doc = Jsoup.connect(prontoUrl).get();

			if (null != doc) {

				Elements divsDirect = doc.select(MigrationAppConstants.FRAME_ELEMENT_SELECT);

				if (null != divsDirect) {

					for (Element elem : divsDirect) {

						String srcElement = elem.attributes().get(MigrationAppConstants.SRC_ATTRIBUTE_SELECT);
						if (null != srcElement && srcElement.contains(MigrationAppConstants.PRONTO_WRITE_URL_PREFIX)) {

							if (srcElement.contains(MigrationAppConstants.PRONTO_URL_PARAM_CHECK)) {

								log.debug("Found success and src is {} for recordId -> {} in processId {}", srcElement,
										recordId, processId);
								return srcElement;
							} else {

								log.error("Found failure and src is {}  for recordId -> {} in processId {}", srcElement,
										recordId, processId);
								return MigrationAppConstants.DOCUMENT_FAILURE_STATUS;
							}
						}
					}
				} else {
					log.error(
							"^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ DivsDirect is null in verifyDocument for Doc {} for recordId {} in processId {} ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^",
							doc.body(), recordId, processId);
				}
			} else {
				log.error(
						"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Doc is null in verifyDocument for recordId {} in processId {} with prontoUrl {} ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~",
						recordId, processId, prontoUrl);
			}

		} catch (IOException exp) {

			log.error(
					"Connection error {} occurred with message {} in fetching document for recordId -> {} in processId {} from pronto via prontoUrl {} in verifyDocument",
					exp.getCause(), exp.getMessage(), recordId, processId, prontoUrl);
			throw new URLConnectionException("Connection error occurred in fetching document from pronto via prontoUrl "
					+ prontoUrl + " for recordId " + recordId + " in processId " + processId, exp.getCause());
		}

		return null;

	}

	public byte[] retrieveDocument(String prontoUrl) {

		log.debug("Calling retrieveDocument with prontoDocumentURL -> {}", migrationProntoUri + prontoUrl);

		return prontoRestTemplate.getForObject(migrationProntoUri + prontoUrl, byte[].class);
	}

}