package com.platform.common.web;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;

public final class PageMeta {

	private final int currentPage;
	private final int perPage;
	private final long total;
	private final int lastPage;
	private final Map<String, Object> additional;

	private PageMeta(int currentPage, int perPage, long total, int lastPage, Map<String, Object> additional) {
		this.currentPage = currentPage;
		this.perPage = perPage;
		this.total = total;
		this.lastPage = lastPage;
		this.additional = Map.copyOf(additional);
	}

	public static PageMeta of(int currentPage, int perPage, long total, int lastPage) {
		return new PageMeta(currentPage, perPage, total, lastPage, Map.of());
	}

	public static PageMeta from(Page<?> page) {
		return of(
			page.getNumber() + 1,
			page.getSize(),
			page.getTotalElements(),
			Math.max(page.getTotalPages(), 1)
		);
	}

	public PageMeta withAdditional(Map<String, ?> additional) {
		return new PageMeta(currentPage, perPage, total, lastPage, new LinkedHashMap<>(additional));
	}

	@JsonProperty("current_page")
	public int currentPage() {
		return currentPage;
	}

	@JsonProperty("per_page")
	public int perPage() {
		return perPage;
	}

	@JsonProperty("total")
	public long total() {
		return total;
	}

	@JsonProperty("last_page")
	public int lastPage() {
		return lastPage;
	}

	@JsonAnyGetter
	public Map<String, Object> additional() {
		return additional;
	}
}
