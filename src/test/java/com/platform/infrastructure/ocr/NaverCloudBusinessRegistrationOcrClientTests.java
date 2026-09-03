package com.platform.infrastructure.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.partner.result.BusinessRegistrationOcrResult;
import com.platform.common.config.BusinessRegistrationOcrProperties;
import org.junit.jupiter.api.Test;

class NaverCloudBusinessRegistrationOcrClientTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final BusinessRegistrationOcrProperties properties = properties();
	private final NaverCloudBusinessRegistrationOcrClient client =
		new NaverCloudBusinessRegistrationOcrClient(properties, objectMapper);

	@Test
	void parsesBusinessRegistrationFields() throws Exception {
		BusinessRegistrationOcrResult result = client.parse(objectMapper.readTree("""
			{
			  "images": [{
			    "inferResult": "SUCCESS",
			    "bizLicense": {"result": {
			      "registerNumber": [{"text": "123-45-67890", "confidenceScore": 0.99}],
			      "companyName": [{"text": "플랫폼뷰티", "confidenceScore": 0.98}],
			      "repName": [{"text": "홍길동", "confidenceScore": 0.97}],
			      "bisAddress": [{"text": "서울특별시 강남구 테헤란로 1", "confidenceScore": 0.94}],
			      "openDate": [{"text": "2025년 12월 27일", "confidenceScore": 0.93}]
			    }}
			  }]
			}
			"""));

		assertThat(result.businessNumber()).isEqualTo("123-45-67890");
		assertThat(result.companyName()).isEqualTo("플랫폼뷰티");
		assertThat(result.ceoName()).isEqualTo("홍길동");
		assertThat(result.businessAddress()).isEqualTo("서울특별시 강남구 테헤란로 1");
		assertThat(result.openingDate()).isEqualTo("2025년 12월 27일");
		assertThat(result.requiresConfirmation()).isFalse();
		assertThat(result.alreadyRegistered()).isFalse();
	}

	@Test
	void requiresConfirmationWhenEssentialFieldConfidenceIsLow() throws Exception {
		BusinessRegistrationOcrResult result = client.parse(objectMapper.readTree("""
			{
			  "images": [{
			    "inferResult": "SUCCESS",
			    "bizLicense": {"result": {
			      "registerNumber": [{"text": "1234567890", "confidenceScore": 0.51}],
			      "corpName": [{"text": "플랫폼 주식회사", "confidenceScore": 0.99}],
			      "headAddress": [{"text": "서울특별시 중구 세종대로 1", "confidenceScore": 0.99}]
			    }}
			  }]
			}
			"""));

		assertThat(result.businessNumber()).isEqualTo("123-45-67890");
		assertThat(result.companyName()).isEqualTo("플랫폼 주식회사");
		assertThat(result.requiresConfirmation()).isTrue();
	}

	private BusinessRegistrationOcrProperties properties() {
		BusinessRegistrationOcrProperties value = new BusinessRegistrationOcrProperties();
		value.setConfirmationConfidence(0.8);
		return value;
	}
}
