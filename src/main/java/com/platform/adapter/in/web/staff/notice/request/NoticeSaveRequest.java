package com.platform.adapter.in.web.staff.notice.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.platform.application.notice.command.SaveNoticeCommand;
import com.platform.domain.notice.NoticeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoticeSaveRequest(@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 100000) String content, @NotNull NoticeStatus status,
	@JsonProperty("publish_start_at") LocalDateTime publishStartAt,
	@JsonProperty("publish_end_at") LocalDateTime publishEndAt, boolean pinned, boolean popup,
	@JsonProperty("attachment_ids") @Size(max = 5) List<@NotNull @Positive Long> attachmentIds,
	@PositiveOrZero Long version) {
	public SaveNoticeCommand toCommand() { return new SaveNoticeCommand(title, content, status, publishStartAt,
		publishEndAt, pinned, popup, attachmentIds == null ? List.of() : attachmentIds, version); }
}
