package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.media.result.MediaResult;
import java.util.List;

public record PartnerOnboardingResult(
	Long id,
	@JsonProperty("allow_status") String allowStatus,
	String status,
	@JsonProperty("rejection_reason") String rejectionReason,
	@JsonProperty("basic_information") BasicInformation basicInformation,
	@JsonProperty("additional_information") AdditionalInformation additionalInformation,
	@JsonProperty("price_information") PriceInformation priceInformation,
	@JsonProperty("verification_information") VerificationInformation verificationInformation
) {

	public record BasicInformation(
		String name,
		String description,
		String industry,
		@JsonProperty("industry_label") String industryLabel,
		@JsonProperty("road_address") String roadAddress,
		@JsonProperty("jibun_address") String jibunAddress,
		@JsonProperty("detail_address") String detailAddress,
		String latitude,
		String longitude,
		@JsonProperty("operating_hours_notice") String operatingHoursNotice,
		@JsonProperty("operation_hours") Object operationHours,
		String direction,
		List<String> hashtags,
		List<PartnerContactResult> contacts,
		MediaResult logo,
		@JsonProperty("main_image") MediaResult mainImage,
		@JsonProperty("interior_images") List<MediaResult> interiorImages
	) {
	}

	public record AdditionalInformation(
		List<PartnerFeatureResult> features,
		List<PartnerLinkResult> links
	) {
	}

	public record PriceInformation(
		List<PartnerOptionResult> options
	) {
	}

	public record VerificationInformation(
		@JsonProperty("business_registration") PartnerBusinessRegistrationResult businessRegistration
	) {
	}
}
