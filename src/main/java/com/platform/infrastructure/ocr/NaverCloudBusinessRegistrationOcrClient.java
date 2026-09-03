package com.platform.infrastructure.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.partner.BusinessRegistrationOcrClient;
import com.platform.application.partner.result.BusinessRegistrationOcrResult;
import com.platform.common.config.BusinessRegistrationOcrProperties;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NaverCloudBusinessRegistrationOcrClient implements BusinessRegistrationOcrClient {

	private static final String OCR_SECRET_HEADER = "X-OCR-SECRET";
	private static final Logger log = LoggerFactory.getLogger(NaverCloudBusinessRegistrationOcrClient.class);
	private final BusinessRegistrationOcrProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public NaverCloudBusinessRegistrationOcrClient(
		BusinessRegistrationOcrProperties properties,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(20));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public BusinessRegistrationOcrResult analyze(MediaFileSource file) {
		if (!properties.configured()) {
			log.warn("Business registration OCR is disabled or missing its invoke URL/secret.");
			throw new ApiException(
				ErrorCode.SERVICE_UNAVAILABLE,
				"사업자등록증 자동 입력을 잠시 사용할 수 없습니다. 기본정보를 직접 입력해 주세요."
			);
		}
		try {
			JsonNode response = request(file);
			return parse(response);
		} catch (ApiException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록증 파일을 읽을 수 없습니다.");
		} catch (RestClientException exception) {
			throw new ApiException(
				ErrorCode.SERVICE_UNAVAILABLE,
				"사업자등록증 인식 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
			);
		}
	}

	private JsonNode request(MediaFileSource file) throws IOException {
		String format = format(file.contentType());
		String message = objectMapper.writeValueAsString(Map.of(
			"version", "V2",
			"requestId", UUID.randomUUID().toString(),
			"timestamp", System.currentTimeMillis(),
			"images", List.of(Map.of("format", format, "name", "business-registration"))
		));
		byte[] bytes;
		try (var input = file.openStream()) {
			bytes = input.readAllBytes();
		}
		ByteArrayResource resource = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return StringUtils.hasText(file.originalFilename())
					? file.originalFilename()
					: "business-registration." + format;
			}
		};
		MultipartBodyBuilder body = new MultipartBodyBuilder();
		body.part("message", message).contentType(MediaType.APPLICATION_JSON);
		body.part("file", resource).contentType(MediaType.parseMediaType(file.contentType()));

		JsonNode response = restClient.post()
			.uri(properties.invokeUrl())
			.header(OCR_SECRET_HEADER, properties.secret())
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(body.build())
			.retrieve()
			.body(JsonNode.class);
		if (response == null) {
			throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "사업자등록증 인식 결과가 없습니다.");
		}
		return response;
	}

	BusinessRegistrationOcrResult parse(JsonNode response) {
		JsonNode image = response.path("images").path(0);
		if (!"SUCCESS".equals(image.path("inferResult").asText())) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"사업자등록증을 인식하지 못했습니다. 선명한 파일로 다시 시도해 주세요."
			);
		}
		JsonNode result = image.path("bizLicense").path("result");
		if (result.isMissingNode() || result.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록증 정보를 찾지 못했습니다.");
		}

		ExtractedField businessNumber = first(result, "registerNumber");
		ExtractedField companyName = first(result, "companyName", "corpName");
		ExtractedField ceoName = first(result, "repName");
		ExtractedField businessAddress = first(result, "bisAddress", "headAddress", "bisArea");
		ExtractedField openingDate = first(result, "openDate");

		Map<String, Double> confidences = new LinkedHashMap<>();
		confidences.put("business_number", businessNumber.confidence());
		confidences.put("company_name", companyName.confidence());
		confidences.put("ceo_name", ceoName.confidence());
		confidences.put("business_address", businessAddress.confidence());
		confidences.put("opening_date", openingDate.confidence());

		boolean requiresConfirmation = List.of(
			businessNumber,
			companyName,
			businessAddress
		).stream().anyMatch(field -> !StringUtils.hasText(field.text())
			|| field.confidence() < properties.confirmationConfidence());

		return new BusinessRegistrationOcrResult(
			formatBusinessNumber(businessNumber.text()),
			companyName.text(),
			ceoName.text(),
			businessAddress.text(),
			openingDate.text(),
			Map.copyOf(confidences),
			requiresConfirmation,
			false
		);
	}

	private ExtractedField first(JsonNode result, String... keys) {
		for (String key : keys) {
			List<ExtractedField> fields = fields(result.path(key));
			if (!fields.isEmpty()) {
				return fields.stream()
					.max(java.util.Comparator.comparingDouble(ExtractedField::confidence))
					.orElse(ExtractedField.empty());
			}
		}
		return ExtractedField.empty();
	}

	private List<ExtractedField> fields(JsonNode array) {
		if (!array.isArray()) {
			return List.of();
		}
		List<ExtractedField> fields = new ArrayList<>();
		for (JsonNode item : array) {
			String text = item.path("text").asText(null);
			if (StringUtils.hasText(text)) {
				fields.add(new ExtractedField(text.trim(), item.path("confidenceScore").asDouble(0)));
			}
		}
		return fields;
	}

	private String format(String contentType) {
		return switch (contentType.toLowerCase(Locale.ROOT)) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "application/pdf" -> "pdf";
			default -> throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"OCR은 JPG, PNG, PDF 사업자등록증만 지원합니다."
			);
		};
	}

	private String formatBusinessNumber(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String digits = value.replaceAll("[^0-9]", "");
		if (digits.length() != 10) {
			return value.trim();
		}
		return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
	}

	private record ExtractedField(String text, double confidence) {
		private static ExtractedField empty() {
			return new ExtractedField(null, 0);
		}
	}
}
