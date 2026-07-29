package com.medi.application.hospital.result;

import com.medi.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HospitalListItemResult(
	Long id,
	String name,
	@JsonProperty("representative_phone") String representativePhone,
	@JsonProperty("view_count") long viewCount,
	@JsonProperty("event_count") long eventCount,
	HospitalEvaluationResult evaluation,
	@JsonProperty("review_counts") HospitalReviewCountsResult reviewCounts,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt,
	MediaResult logo,
	HospitalAccountResult account,
	List<HospitalCategoryResult> categories,
	List<HospitalFeatureResult> features
) {
}
