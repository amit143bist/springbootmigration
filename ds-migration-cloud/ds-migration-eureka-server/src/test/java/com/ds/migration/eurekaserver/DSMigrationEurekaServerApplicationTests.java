package com.ds.migration.eurekaserver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles(value = "unittest")
public class DSMigrationEurekaServerApplicationTests {

	@Test
	public void contextLoads() {
	}

}