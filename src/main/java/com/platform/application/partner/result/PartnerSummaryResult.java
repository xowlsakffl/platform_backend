package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerSummaryResult(
	@JsonProperty("dormant_partners") long dormantPartners,
	@JsonProperty("pending_review_partners") long pendingReviewPartners,
	@JsonProperty("rejected_review_partners") long rejectedReviewPartners,
	@JsonProperty("suspended_partners") long suspendedPartners,
	@JsonProperty("withdrawn_partners") long withdrawnPartners
) {
}
