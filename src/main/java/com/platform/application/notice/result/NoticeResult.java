package com.platform.application.notice.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.platform.domain.notice.NoticePublicationStatus;
import com.platform.domain.notice.NoticeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeResult(Long id, long version, String title, String content, NoticeStatus status,
	@JsonProperty("publication_status") NoticePublicationStatus publicationStatus,
	@JsonProperty("publish_start_at") LocalDateTime publishStartAt,
	@JsonProperty("publish_end_at") LocalDateTime publishEndAt,
	boolean pinned, boolean popup, @JsonProperty("author_name") String authorName,
	@JsonProperty("created_at") LocalDateTime createdAt, @JsonProperty("updated_at") LocalDateTime updatedAt,
	List<NoticeFileResult> attachments) {}
