package com.medi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediBackendApplication.class, args);
	}

}
