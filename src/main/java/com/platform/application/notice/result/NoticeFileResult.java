package com.platform.application.notice.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NoticeFileResult(Long id, @JsonProperty("original_name") String originalName,
	@JsonProperty("mime_type") String mimeType, long size, String url) {}
