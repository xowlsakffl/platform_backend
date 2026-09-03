package com.platform.adapter.in.web.partner.onboarding.request;

import com.platform.adapter.in.web.staff.partner.request.PartnerRequestSupport;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import com.platform.application.partner.command.UpdatePartnerOnboardingCommand;
import com.platform.common.web.multipart.MultipartMediaFileSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

public record PartnerOnboardingUpdateRequest(
	@Size(max = 30) String name,
	@BindParam("english_name") @Size(max = 90) String englishName,
	@Size(max = 2000) String description,
	@BindParam("category_id") @Positive Long categoryId,
	@BindParam("road_address") @Size(max = 255) String roadAddress,
	@BindParam("detail_address") @Size(max = 255) String detailAddress,
	@DecimalMin("-90") @DecimalMax("90") String latitude,
	@DecimalMin("-180") @DecimalMax("180") String longitude,
	@BindParam("subway_stations") @Size(max = 12000) String subwayStations,
	@BindParam("operating_hours_notice") @Size(max = 500) String operatingHoursNotice,
	@BindParam("operation_hours") String operationHours,
	@BindParam("holiday_policy") String holidayPolicy,
	@Size(max = 5000) String direction,
	@BindParam("representative_phone") @Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String representativePhone,
	@BindParam("representative_email") @Email @Size(max = 255)
	String representativeEmail,
	@BindParam("sms_sender_phone") @Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String smsSenderPhone,
	@BindParam("call_receiver_phone") @Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String callReceiverPhone,
	@BindParam("consultation_receiver_phones[]") @Size(max = 3)
	List<@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN) String> consultationReceiverPhones,
	@BindParam("event_notice_receiver_phones[]") @Size(max = 3)
	List<@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN) String> eventNoticeReceiverPhones,
	@BindParam("notice_marketing_emails[]") @Size(max = 3)
	List<@Email @Size(max = 255) String> noticeMarketingEmails,
	@BindParam("business_number")
	@Pattern(regexp = PartnerRequestSupport.BUSINESS_NUMBER_PATTERN) String businessNumber,
	@BindParam("company_name") @Size(max = 255) String companyName,
	@BindParam("ceo_name") @Size(max = 100) String ceoName,
	@BindParam("opening_date") LocalDate openingDate,
	@BindParam("feature_ids[]") @Size(max = 100) Set<@Positive Long> featureIds,
	@BindParam("hashtags[]") @Size(max = 10) List<@Size(max = 30) String> hashtags,
	@Size(max = 12000) String links,
	MultipartFile logo,
	@BindParam("existing_logo_id") @Positive Long existingLogoId,
	@BindParam("main_image") MultipartFile mainImage,
	@BindParam("existing_main_image_id") @Positive Long existingMainImageId,
	@BindParam("interior_images[]") @Size(max = 9) List<MultipartFile> interiorImages,
	@BindParam("existing_interior_image_ids[]") @Size(max = 9) List<@Positive Long> existingInteriorImageIds,
	@BindParam("interior_image_order[]") @Size(max = 9)
	List<@Pattern(regexp = "^(existing|new):[0-9]+$") String> interiorImageOrder,
	@BindParam("business_registration_file") MultipartFile businessRegistrationFile,
	@BindParam("existing_business_registration_file_id") @Positive Long existingBusinessRegistrationFileId
) {

	public UpdatePartnerOnboardingCommand toCommand(Set<String> requestFields) {
		Set<String> fields = normalizeFields(requestFields);
		return new UpdatePartnerOnboardingCommand(
			name,
			englishName,
			description,
			categoryId,
			roadAddress,
			detailAddress,
			latitude,
			longitude,
			subwayStations,
			operatingHoursNotice,
			operationHours,
			holidayPolicy,
			direction,
			contactsOrNull(fields),
			businessRegistrationOrNull(fields),
			fields.contains("feature_ids") ? PartnerRequestSupport.ids(featureIds) : null,
			fields.contains("hashtags") ? (hashtags == null ? List.of() : hashtags) : null,
			fields.contains("links") ? links : null,
			MultipartMediaFileSource.from(logo),
			existingLogoId,
			MultipartMediaFileSource.from(mainImage),
			existingMainImageId,
			interiorImages == null
				? List.of()
				: interiorImages.stream().map(MultipartMediaFileSource::from).toList(),
			existingInteriorImageIds,
			interiorImageOrder,
			MultipartMediaFileSource.from(businessRegistrationFile),
			existingBusinessRegistrationFileId,
			fields
		);
	}

	private PartnerContactSetCommand contactsOrNull(Set<String> fields) {
		if (Set.of(
			"representative_phone",
			"representative_email",
			"sms_sender_phone",
			"call_receiver_phone",
			"consultation_receiver_phones",
			"event_notice_receiver_phones",
			"notice_marketing_emails"
		).stream().noneMatch(fields::contains)) {
			return null;
		}
		return new PartnerContactSetCommand(
			representativePhone,
			representativeEmail,
			smsSenderPhone,
			callReceiverPhone,
			PartnerRequestSupport.list(consultationReceiverPhones),
			PartnerRequestSupport.list(eventNoticeReceiverPhones),
			PartnerRequestSupport.list(noticeMarketingEmails)
		);
	}

	private PartnerBusinessRegistrationCommand businessRegistrationOrNull(Set<String> fields) {
		if (Set.of(
			"business_number",
			"company_name",
			"ceo_name",
			"opening_date"
		).stream().noneMatch(fields::contains)) {
			return null;
		}
		return new PartnerBusinessRegistrationCommand(
			businessNumber,
			companyName,
			ceoName,
			openingDate,
			null,
			null,
			null
		);
	}

	private Set<String> normalizeFields(Set<String> requestFields) {
		Set<String> fields = new LinkedHashSet<>();
		for (String field : requestFields) {
			fields.add(field.endsWith("[]") ? field.substring(0, field.length() - 2) : field);
		}
		if (logo != null && !logo.isEmpty()) {
			fields.add("logo");
		}
		if (mainImage != null && !mainImage.isEmpty()) {
			fields.add("main_image");
		}
		if (interiorImages != null && interiorImages.stream().anyMatch(file -> file != null && !file.isEmpty())) {
			fields.add("interior_images");
		}
		if (businessRegistrationFile != null && !businessRegistrationFile.isEmpty()) {
			fields.add("business_registration_file");
		}
		return Set.copyOf(fields);
	}
}
