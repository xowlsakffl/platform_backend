package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerLinkResult(
	Long id,
	String type,
	String url,
	@JsonProperty("sort_order") int sortOrder
) {
}
