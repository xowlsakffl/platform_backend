package com.platform.application.notice.query;

import com.platform.domain.notice.NoticePublicationStatus;
import java.time.LocalDate;

public record SearchNoticesQuery(String search, NoticePublicationStatus publicationStatus,
	Boolean popup, LocalDate createdFrom, LocalDate createdTo, int page, int perPage) {}
