package com.medi;

import org.springframework.boot.SpringApplication;

public class TestMediBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MediBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
