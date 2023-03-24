package com.ds.migration.website.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
@AutoConfigureMockMvc
@Slf4j
public class OracleIBISServiceTest {

    @Autowired
    OracleIBISService oracleIBISService;

    @Test
    public void getEnvelopeByDocument() {

        String envelopeId = oracleIBISService.getEnvelopeByDocument(11221222l, "AEDB055DAE256D8C85209FC3C5B50CA088BF60AE");
        Assert.assertEquals("ca53c6ed-f94e-410d-8694-e215b796cb07", envelopeId);
    }
}