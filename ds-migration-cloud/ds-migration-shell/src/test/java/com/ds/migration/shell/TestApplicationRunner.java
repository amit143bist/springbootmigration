package com.ds.migration.shell;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;

import lombok.extern.slf4j.Slf4j;

@TestConfiguration
@Slf4j
public class TestApplicationRunner implements ApplicationRunner {


    public TestApplicationRunner() {
        log.info("Test Application Runner started!");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("About to do nothing!");
    }

}