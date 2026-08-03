package com.platform.common.web;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PaginatedResponse<T>(List<T> items, PageMeta meta) {

	public static <S, T> PaginatedResponse<T> from(Page<S> page, Function<S, T> mapper) {
		return from(page, mapper, Map.of());
	}

	public static <S, T> PaginatedResponse<T> from(
		Page<S> page,
		Function<S, T> mapper,
		Map<String, ?> additionalMeta
	) {
		return new PaginatedResponse<>(
			page.getContent().stream().map(mapper).toList(),
			PageMeta.from(page).withAdditional(additionalMeta)
		);
	}
}
