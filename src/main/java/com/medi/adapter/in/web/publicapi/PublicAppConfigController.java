package com.medi.adapter.in.web.publicapi;

import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
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
	ApiResponse.Success<AppConfigResponse> getAppConfig(HttpServletRequest request) {
		return ApiResponse.success(new AppConfigResponse(brandName, serviceName), RequestTrace.traceId(request));
	}

	record AppConfigResponse(String brandName, String serviceName) {
	}
}
