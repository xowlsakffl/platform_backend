package com.platform.application.notice.command;

import com.platform.domain.notice.NoticeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SaveNoticeCommand(String title, String content, NoticeStatus status,
	LocalDateTime publishStartAt, LocalDateTime publishEndAt, boolean pinned, boolean popup,
	List<Long> attachmentIds, Long version) {}
