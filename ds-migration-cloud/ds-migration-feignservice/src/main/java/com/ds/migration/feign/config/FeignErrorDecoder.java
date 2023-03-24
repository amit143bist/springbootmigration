package com.ds.migration.feign.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ds.migration.common.exception.ErrorDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

	private ObjectMapper objectMapper;

	@Override
	public Exception decode(String methodKey, Response response) {

		log.error("methodKey is {}, response.status is {}, response.reason is {}", methodKey, response.status(),
				response.reason());

		ErrorDetails errorDetails = null;
		try {

			String body = Util.toString(response.body().asReader());
			errorDetails = objectMapper.readValue(body, ErrorDetails.class);

			log.error("Timestamp is {}, details is {}, message is {}", errorDetails.getTimestamp(),
					errorDetails.getDetails(), errorDetails.getMessage());

			return new ResponseStatusException(HttpStatus.valueOf(response.status()),
					errorDetails.getTimestamp() + "_" + errorDetails.getMessage());
		} catch (IOException ex) {

			log.error("IOException {} in FeignErrorDecoder.decode() with errorMessage -> {} ", ex.getCause(),
					ex.getMessage());
		}

		return null;
	}

}