package com.medi.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
class PublicAppConfigController {

	private final String brandName;
	private final String serviceName;

	PublicAppConfigController(
		@Value("${app.brand.name}") String brandName,
		@Value("${app.brand.service-name}") String serviceName
	) {
		this.brandName = brandName;
		this.serviceName = serviceName;
	}

	@GetMapping("/app-config")
	AppConfigResponse getAppConfig() {
		return new AppConfigResponse(brandName, serviceName);
	}

	record AppConfigResponse(String brandName, String serviceName) {
	}
}
