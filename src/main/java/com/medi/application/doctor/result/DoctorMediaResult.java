package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record DoctorMediaResult(
	Long id,
	String url,
	@JsonProperty("mime_type") String mimeType,
	long size,
	Integer width,
	Integer height,
	Map<String, Object> metadata
) {
}
