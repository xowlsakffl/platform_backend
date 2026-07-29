package com.medi.adapter.in.web.staff.hospital.request;

import com.medi.common.web.multipart.MultipartMediaFileSource;
import com.medi.application.hospital.command.CreateHospitalCommand;
import com.medi.application.hospital.command.HospitalBusinessRegistrationCommand;
import com.medi.application.hospital.command.HospitalContactSetCommand;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalInterpretationLanguage;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record HospitalCreateForStaffRequest(
	@NotBlank @Size(max = 255) String name,
	@Size(max = 5000) String description,
	@BindParam("youtube_link") @Size(max = 500) String youtubeLink,
	@NotBlank @Size(max = 255) String address,
	@BindParam("address_detail") @NotBlank @Size(max = 255) String addressDetail,
	@NotBlank @DecimalMin("-90") @DecimalMax("90") String latitude,
	@NotBlank @DecimalMin("-180") @DecimalMax("180") String longitude,
	@BindParam("consulting_hours") @Size(max = 5000) String consultingHours,
	@BindParam("operation_hours") @NotNull Object operationHours,
	@Size(max = 5000) String direction,
	@BindParam("allow_status") @NotNull HospitalAllowStatus allowStatus,
	@NotNull HospitalStatus status,
	@BindParam("representative_phone")
	@NotBlank
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String representativePhone,
	@BindParam("sms_sender_phone")
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String smsSenderPhone,
	@BindParam("call_receiver_phone")
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String callReceiverPhone,
	@BindParam("consultation_receiver_phones[]")
	@Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> consultationReceiverPhones,
	@BindParam("event_notice_receiver_phones[]")
	@Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> eventNoticeReceiverPhones,
	@BindParam("notice_marketing_emails[]") @Size(max = 3) List<@Email String> noticeMarketingEmails,
	@BindParam("business_number") @NotBlank @Size(max = 20) String businessNumber,
	@BindParam("company_name") @NotBlank @Size(max = 255) String companyName,
	@BindParam("ceo_name") @NotBlank @Size(max = 100) String ceoName,
	@BindParam("business_type") @NotBlank @Size(max = 100) String businessType,
	@BindParam("business_item") @NotBlank @Size(max = 100) String businessItem,
	@BindParam("business_address") @Size(max = 255) String businessAddress,
	@BindParam("business_address_detail") @Size(max = 255) String businessAddressDetail,
	@BindParam("settlement_bank_name") @Size(max = 50) String settlementBankName,
	@BindParam("settlement_account_number") @Size(max = 50)
	@Pattern(regexp = "^[0-9\\-\\s]{2,50}$") String settlementAccountNumber,
	@BindParam("settlement_account_holder") @Size(max = 100) String settlementAccountHolder,
	@BindParam("tax_invoice_email") @Email @Size(max = 255) String taxInvoiceEmail,
	@BindParam("issued_at") LocalDate issuedAt,
	@BindParam("category_ids[]") @Size(min = 1, max = 5) Set<@Positive Long> categoryIds,
	@BindParam("feature_ids[]") @NotEmpty @Size(max = 100) Set<@Positive Long> featureIds,
	@BindParam("interpretation_languages[]") @Size(max = 5)
	Set<@NotNull HospitalInterpretationLanguage> interpretationLanguages,
	@NotNull MultipartFile logo,
	@NotEmpty @Size(max = 5) List<MultipartFile> gallery,
	@BindParam("business_registration_file") @NotNull MultipartFile businessRegistrationFile
) {

	public CreateHospitalCommand toCommand() {
		return new CreateHospitalCommand(
			name,
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
			contacts(),
			businessRegistration(),
			HospitalRequestSupport.ids(categoryIds),
			HospitalRequestSupport.ids(featureIds),
			interpretationLanguages == null ? Set.of() : Set.copyOf(interpretationLanguages),
			MultipartMediaFileSource.from(logo),
			gallery.stream().map(MultipartMediaFileSource::from).toList(),
			MultipartMediaFileSource.from(businessRegistrationFile)
		);
	}

	private HospitalContactSetCommand contacts() {
		return new HospitalContactSetCommand(
			representativePhone,
			smsSenderPhone,
			callReceiverPhone,
			HospitalRequestSupport.list(consultationReceiverPhones),
			HospitalRequestSupport.list(eventNoticeReceiverPhones),
			HospitalRequestSupport.list(noticeMarketingEmails)
		);
	}

	private HospitalBusinessRegistrationCommand businessRegistration() {
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
}
