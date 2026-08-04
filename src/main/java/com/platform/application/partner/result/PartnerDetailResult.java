package com.platform.application.partner.result;

import com.platform.application.media.result.MediaResult;
import com.platform.application.category.result.CategoryReferenceResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartnerDetailResult(
	Long id,
	String name,
	String description,
	@JsonProperty("road_address") String roadAddress,
	@JsonProperty("jibun_address") String jibunAddress,
	String latitude,
	String longitude,
	PartnerContactGroupResult contacts,
	@JsonProperty("contact_items") List<PartnerContactResult> contactItems,
	@JsonProperty("operating_hours_notice") String operatingHoursNotice,
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
	@JsonProperty("main_image") MediaResult mainImage,
	@JsonProperty("interior_images") List<MediaResult> interiorImages,
	List<PartnerFeatureResult> features,
	@JsonProperty("assigned_staff") PartnerAssignedStaffResult assignedStaff,
	@JsonProperty("account_partner") PartnerAccountResult accountPartner,
	List<PartnerSpecialistForStaffResult> specialists,
	@JsonProperty("business_registration") PartnerBusinessRegistrationResult businessRegistration,
	List<CategoryReferenceResult> categories,
	@JsonProperty("detail_address") String detailAddress,
	List<String> hashtags,
	List<PartnerLinkResult> links,
	List<PartnerOptionResult> options,
	@JsonProperty("registration_source") String registrationSource,
	@JsonProperty("created_by_staff_id") Long createdByStaffId,
	@JsonProperty("account_link_status") String accountLinkStatus,
	@JsonProperty("latest_account_invitation") PartnerAccountInvitationResult latestAccountInvitation
) {
}
