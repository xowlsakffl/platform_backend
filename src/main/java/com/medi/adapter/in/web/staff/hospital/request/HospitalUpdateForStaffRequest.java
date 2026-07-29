package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.common.web.multipart.MultipartMediaFileSource;
import com.medi.application.hospital.command.HospitalBusinessRegistrationCommand;
import com.medi.application.hospital.command.HospitalContactSetCommand;
import com.medi.application.hospital.command.UpdateHospitalCommand;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalInterpretationLanguage;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record HospitalUpdateForStaffRequest(
	@Size(max = 5000) String description,
	@BindParam("youtube_link") @Size(max = 500) String youtubeLink,
	@Pattern(regexp = ".*\\S.*") @Size(max = 255) String address,
	@BindParam("address_detail") @Pattern(regexp = ".*\\S.*") @Size(max = 255) String addressDetail,
	@DecimalMin("-90") @DecimalMax("90") String latitude,
	@DecimalMin("-180") @DecimalMax("180") String longitude,
	@BindParam("consulting_hours") @Size(max = 5000) String consultingHours,
	@BindParam("operation_hours") Object operationHours,
	@Size(max = 5000) String direction,
	@BindParam("allow_status") HospitalAllowStatus allowStatus,
	HospitalStatus status,
	@BindParam("representative_phone") @Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String representativePhone,
	@BindParam("sms_sender_phone") @Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String smsSenderPhone,
	@BindParam("call_receiver_phone") @Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String callReceiverPhone,
	@BindParam("consultation_receiver_phones[]") @Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> consultationReceiverPhones,
	@BindParam("event_notice_receiver_phones[]") @Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> eventNoticeReceiverPhones,
	@BindParam("notice_marketing_emails[]") @Size(max = 3) List<@Email String> noticeMarketingEmails,
	@BindParam("business_number") @Size(max = 20) String businessNumber,
	@BindParam("company_name") @Size(max = 255) String companyName,
	@BindParam("ceo_name") @Size(max = 100) String ceoName,
	@BindParam("business_type") @Size(max = 100) String businessType,
	@BindParam("business_item") @Size(max = 100) String businessItem,
	@BindParam("business_address") @Size(max = 255) String businessAddress,
	@BindParam("business_address_detail") @Size(max = 255) String businessAddressDetail,
	@BindParam("settlement_bank_name") @Size(max = 50) String settlementBankName,
	@BindParam("settlement_account_number") @Size(max = 50)
	@Pattern(regexp = "^[0-9\\-\\s]{2,50}$") String settlementAccountNumber,
	@BindParam("settlement_account_holder") @Size(max = 100) String settlementAccountHolder,
	@BindParam("tax_invoice_email") @Email @Size(max = 255) String taxInvoiceEmail,
	@BindParam("issued_at") LocalDate issuedAt,
	@BindParam("category_ids[]") @Size(min = 1, max = 5) Set<@Positive Long> categoryIds,
	@BindParam("feature_ids[]") @Size(min = 1, max = 100) Set<@Positive Long> featureIds,
	@BindParam("interpretation_languages[]") @Size(max = 5)
	Set<@NotNull HospitalInterpretationLanguage> interpretationLanguages,
	MultipartFile logo,
	@BindParam("existing_logo_id") @Positive Long existingLogoId,
	@Size(max = 5) List<MultipartFile> gallery,
	@BindParam("existing_gallery_ids[]") @Size(max = 5) List<@Positive Long> existingGalleryIds,
	@BindParam("gallery_order[]") @Size(max = 5)
	List<@Pattern(regexp = "^(existing|new):[0-9]+$") String> galleryOrder,
	@BindParam("business_registration_file") MultipartFile businessRegistrationFile,
	@BindParam("existing_business_registration_file_id") @Positive Long existingBusinessRegistrationFileId
) {

	public UpdateHospitalCommand toCommand(Set<String> requestFields) {
		Set<String> fields = normalizeFields(requestFields);
		return new UpdateHospitalCommand(
			description,
			youtubeLink,
			address,
			addressDetail,
			latitude,
			longitude,
			consultingHours,
			operationHours,
			direction,
			allowStatus,
			status,
			contactsOrNull(fields),
			businessRegistrationOrNull(fields),
			fields.contains("category_ids") ? HospitalRequestSupport.ids(categoryIds) : null,
			fields.contains("feature_ids") ? HospitalRequestSupport.ids(featureIds) : null,
			fields.contains("interpretation_languages")
				? interpretationLanguages == null ? Set.of() : Set.copyOf(interpretationLanguages)
				: null,
			MultipartMediaFileSource.from(logo),
			existingLogoId,
			gallery == null ? List.of() : gallery.stream().map(MultipartMediaFileSource::from).toList(),
			existingGalleryIds,
			galleryOrder,
			MultipartMediaFileSource.from(businessRegistrationFile),
			existingBusinessRegistrationFileId,
			fields
		);
	}

	private HospitalContactSetCommand contactsOrNull(Set<String> fields) {
		if (Set.of(
			"representative_phone",
			"sms_sender_phone",
			"call_receiver_phone",
			"consultation_receiver_phones",
			"event_notice_receiver_phones",
			"notice_marketing_emails"
		).stream().noneMatch(fields::contains)) {
			return null;
		}
		return new HospitalContactSetCommand(
			representativePhone,
			smsSenderPhone,
			callReceiverPhone,
			HospitalRequestSupport.list(consultationReceiverPhones),
			HospitalRequestSupport.list(eventNoticeReceiverPhones),
			HospitalRequestSupport.list(noticeMarketingEmails)
		);
	}

	private HospitalBusinessRegistrationCommand businessRegistrationOrNull(Set<String> fields) {
		if (Set.of(
			"business_number",
			"company_name",
			"ceo_name",
			"business_type",
			"business_item",
			"business_address",
			"business_address_detail",
			"settlement_bank_name",
			"settlement_account_number",
			"settlement_account_holder",
			"tax_invoice_email",
			"issued_at"
		).stream().noneMatch(fields::contains)) {
			return null;
		}
		return new HospitalBusinessRegistrationCommand(
			businessNumber,
			companyName,
			ceoName,
			businessType,
			businessItem,
			businessAddress,
			businessAddressDetail,
			settlementBankName,
			settlementAccountNumber,
			settlementAccountHolder,
			taxInvoiceEmail,
			issuedAt
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
		if (gallery != null && gallery.stream().anyMatch(file -> file != null && !file.isEmpty())) {
			fields.add("gallery");
		}
		if (businessRegistrationFile != null && !businessRegistrationFile.isEmpty()) {
			fields.add("business_registration_file");
		}
		return Set.copyOf(fields);
	}
}
