package com.medi.application.hospital.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HospitalSummaryResult(
	@JsonProperty("dormant_hospitals") long dormantHospitals,
	@JsonProperty("pending_review_hospitals") long pendingReviewHospitals,
	@JsonProperty("rejected_review_hospitals") long rejectedReviewHospitals,
	@JsonProperty("suspended_hospitals") long suspendedHospitals,
	@JsonProperty("withdrawn_hospitals") long withdrawnHospitals
) {
}
