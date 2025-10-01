package com.hethmon.bbq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

@Slf4j
@SpringBootTest
@Profile("test")
class BbqApplicationTests {

	@Test
	void contextLoads() {
	}

}
