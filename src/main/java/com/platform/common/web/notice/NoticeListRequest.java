package com.platform.common.web.notice;

import com.platform.application.notice.query.SearchNoticesQuery;
import com.platform.domain.notice.NoticePublicationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record NoticeListRequest(@Size(max = 100) String search, NoticePublicationStatus publication_status,
	Boolean popup, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created_from,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate created_to,
	@Min(1) Integer page, @Min(1) @Max(100) Integer per_page) {
	public int pageValue() { return page == null ? 1 : page; }
	public int perPageValue() { return per_page == null ? 20 : per_page; }
	public SearchNoticesQuery toQuery() { return new SearchNoticesQuery(search, publication_status, popup, created_from, created_to, pageValue(), perPageValue()); }
}
