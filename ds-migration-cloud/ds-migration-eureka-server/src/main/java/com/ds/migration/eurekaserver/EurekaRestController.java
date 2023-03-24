package com.ds.migration.eurekaserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class EurekaRestController {

	@Autowired
	Environment environment;

	@GetMapping("/testservice")
	public String testservice() {

		log.info("Example info log from {}", EurekaRestController.class.getSimpleName());

		log.debug("Example debug log from {}", EurekaRestController.class.getSimpleName());

		String serverPort = environment.getProperty("local.server.port");

		log.info("Port log from {}", serverPort);

		log.debug("Port log from {}", serverPort);

		return "Hello form Backend!!! " + " Host : localhost " + " :: Port : " + serverPort;
	}
}