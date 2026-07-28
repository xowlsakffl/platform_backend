package com.medi.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {

	@Bean
	ObjectMapper objectMapper() {
		return JsonMapper.builder()
			.findAndAddModules()
			.build();
	}
}
