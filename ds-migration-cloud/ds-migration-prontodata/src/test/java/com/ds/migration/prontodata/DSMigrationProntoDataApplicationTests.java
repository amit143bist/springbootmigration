package com.ds.migration.prontodata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "unittest")
@TestPropertySource(locations = "classpath:application-unittest.yml")
public class DSMigrationProntoDataApplicationTests {

	@Test
	public void contextLoads() {
	}

}