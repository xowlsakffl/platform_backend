package com.platform.application.specialist.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SpecialistMediaResult(
	Long id,
	String url,
	@JsonProperty("mime_type") String mimeType,
	long size,
	Integer width,
	Integer height,
	Map<String, Object> metadata
) {
}
