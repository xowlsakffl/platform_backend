package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PartnerAccessInformationValidator {

	private static final int MAX_SUBWAY_STATIONS = 2;
	private final ObjectMapper objectMapper;

	public PartnerAccessInformationValidator(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String normalizeSubwayStations(Object value) {
		JsonNode root = arrayNode(value);
		if (root.size() > MAX_SUBWAY_STATIONS) {
			throw invalid("지하철역은 최대 2개까지 등록할 수 있습니다.");
		}

		ArrayNode normalized = objectMapper.createArrayNode();
		Set<String> uniqueStations = new HashSet<>();
		for (int index = 0; index < root.size(); index++) {
			JsonNode station = root.get(index);
			if (!station.isObject()) {
				throw invalid("지하철역 정보 형식이 올바르지 않습니다.");
			}
			String name = requiredText(station, "name", 100);
			String externalId = optionalText(station, "external_id", 100);
			String uniqueKey = externalId == null ? name : externalId;
			if (!uniqueStations.add(uniqueKey)) {
				throw invalid("같은 지하철역을 중복 등록할 수 없습니다.");
			}

			ObjectNode item = normalized.addObject();
			putNullable(item, "external_id", externalId);
			item.put("name", name);
			putNullable(item, "line", optionalText(station, "line", 100));
			putCoordinate(item, station, "latitude", new BigDecimal("-90"), new BigDecimal("90"));
			putCoordinate(item, station, "longitude", new BigDecimal("-180"), new BigDecimal("180"));
			putDistance(item, station.path("distance_meters"));
			item.put("sort_order", index);
		}

		return write(normalized);
	}

	private JsonNode arrayNode(Object value) {
		if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
			return objectMapper.createArrayNode();
		}
		try {
			JsonNode root = value instanceof String rawValue
				? objectMapper.readTree(rawValue)
				: objectMapper.valueToTree(value);
			if (root == null || !root.isArray()) {
				throw invalid("지하철역 정보 형식이 올바르지 않습니다.");
			}
			return root;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw invalid("지하철역 정보 형식이 올바르지 않습니다.");
		}
	}

	private void putCoordinate(
		ObjectNode target,
		JsonNode source,
		String field,
		BigDecimal minimum,
		BigDecimal maximum
	) {
		JsonNode value = source.path(field);
		if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
			target.putNull(field);
			return;
		}
		try {
			BigDecimal coordinate = new BigDecimal(value.asText());
			if (coordinate.compareTo(minimum) < 0 || coordinate.compareTo(maximum) > 0) {
				throw invalid("지하철역 좌표가 올바르지 않습니다.");
			}
			target.put(field, coordinate.stripTrailingZeros().toPlainString());
		} catch (NumberFormatException exception) {
			throw invalid("지하철역 좌표가 올바르지 않습니다.");
		}
	}

	private void putDistance(ObjectNode target, JsonNode value) {
		if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
			target.putNull("distance_meters");
			return;
		}
		try {
			int distance = Integer.parseInt(value.asText());
			if (distance < 0 || distance > 100_000) {
				throw invalid("지하철역 거리는 0m 이상 100,000m 이하로 입력해 주세요.");
			}
			target.put("distance_meters", distance);
		} catch (NumberFormatException exception) {
			throw invalid("지하철역 거리는 숫자로 입력해 주세요.");
		}
	}

	private String requiredText(JsonNode node, String field, int maxLength) {
		String value = optionalText(node, field, maxLength);
		if (value == null) {
			throw invalid("지하철역 이름을 입력해 주세요.");
		}
		return value;
	}

	private String optionalText(JsonNode node, String field, int maxLength) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
			return null;
		}
		if (!value.isTextual() || value.asText().trim().length() > maxLength) {
			throw invalid("지하철역 정보가 허용 길이를 초과했습니다.");
		}
		return value.asText().trim();
	}

	private void putNullable(ObjectNode node, String field, String value) {
		if (value == null) {
			node.putNull(field);
		} else {
			node.put(field, value);
		}
	}

	private String write(JsonNode value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw invalid("지하철역 정보를 저장할 수 없습니다.");
		}
	}

	private ApiException invalid(String message) {
		return new ApiException(ErrorCode.INVALID_REQUEST, message);
	}
}
