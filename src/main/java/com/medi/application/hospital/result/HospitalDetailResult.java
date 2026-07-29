package com.medi.application.hospital.result;

import com.medi.application.media.result.MediaResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HospitalDetailResult(
	Long id,
	String name,
	String description,
	@JsonProperty("youtube_link") String youtubeLink,
	String address,
	@JsonProperty("address_detail") String addressDetail,
	String latitude,
	String longitude,
	HospitalContactGroupResult contacts,
	@JsonProperty("contact_items") List<HospitalContactResult> contactItems,
	@JsonProperty("consulting_hours") String consultingHours,
	@JsonProperty("operation_hours") Object operationHours,
	String direction,
	@JsonProperty("view_count") long viewCount,
	@JsonProperty("new_event_db_count") long newEventDbCount,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("latest_status_history") OperationHistoryResult latestStatusHistory,
	@JsonProperty("created_at") LocalDateTime createdAt,
	@JsonProperty("updated_at") LocalDateTime updatedAt,
	MediaResult logo,
	List<MediaResult> gallery,
	List<HospitalCategoryResult> categories,
	List<HospitalFeatureResult> features,
	@JsonProperty("interpretation_languages")
	List<HospitalInterpretationLanguageResult> interpretationLanguages,
	@JsonProperty("account_hospital") HospitalAccountResult accountHospital,
	List<HospitalDoctorForStaffResult> doctors,
	@JsonProperty("business_registration") HospitalBusinessRegistrationResult businessRegistration
) {
}
