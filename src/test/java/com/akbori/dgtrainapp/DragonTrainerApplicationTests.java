package com.akbori.dgtrainapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// An in-memory database keeps the test independent of the local data/drogontrainapp.db.
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:sqlite:file:contexttest?mode=memory&cache=shared"
})
class DragonTrainerApplicationTests {

	@Test
	void contextLoads() {
	}

}