package com.platform.adapter.in.web.staff.partner.request;

import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.application.partner.command.CreatePartnerCommand;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.partner.PartnerIndustry;
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

public record PartnerCreateForStaffRequest(
	@NotBlank @Size(max = 255) String name,
	@Size(max = 2000) String description,
	@NotNull PartnerIndustry industry,
	@BindParam("road_address") @NotBlank @Size(max = 255) String roadAddress,
	@BindParam("jibun_address") @NotBlank @Size(max = 255) String jibunAddress,
	@BindParam("detail_address") @Size(max = 255) String detailAddress,
	@NotBlank @DecimalMin("-90") @DecimalMax("90") String latitude,
	@NotBlank @DecimalMin("-180") @DecimalMax("180") String longitude,
	@BindParam("operating_hours_notice") @Size(max = 5000) String operatingHoursNotice,
	@BindParam("operation_hours") @NotNull Object operationHours,
	@Size(max = 5000) String direction,
	@BindParam("allow_status") @NotNull PartnerAllowStatus allowStatus,
	@NotNull PartnerStatus status,
	@BindParam("representative_phone")
	@NotBlank
	@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String representativePhone,
	@BindParam("sms_sender_phone")
	@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String smsSenderPhone,
	@BindParam("call_receiver_phone")
	@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String callReceiverPhone,
	@BindParam("consultation_receiver_phones[]")
	@Size(max = 3)
	List<@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN) String> consultationReceiverPhones,
	@BindParam("event_notice_receiver_phones[]")
	@Size(max = 3)
	List<@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN) String> eventNoticeReceiverPhones,
	@BindParam("notice_marketing_emails[]") @Size(max = 3)
	List<@Email @Size(max = 255) String> noticeMarketingEmails,
	@BindParam("business_number") @NotBlank @Size(max = 20) String businessNumber,
	@BindParam("company_name") @NotBlank @Size(max = 255) String companyName,
	@BindParam("ceo_name") @NotBlank @Size(max = 100) String ceoName,
	@BindParam("business_type") @NotBlank @Size(max = 100) String businessType,
	@BindParam("business_item") @NotBlank @Size(max = 100) String businessItem,
	@BindParam("business_address") @Size(max = 255) String businessAddress,
	@BindParam("business_address_detail") @Size(max = 255) String businessAddressDetail,
	@BindParam("settlement_bank_name") @Size(max = 50) String settlementBankName,
	@BindParam("settlement_account_number") @Size(max = 50)
	@Pattern(regexp = "^(?:|[0-9\\-\\s]{2,50})$") String settlementAccountNumber,
	@BindParam("settlement_account_holder") @Size(max = 100) String settlementAccountHolder,
	@BindParam("tax_invoice_email") @Email @Size(max = 255) String taxInvoiceEmail,
	@BindParam("issued_at") LocalDate issuedAt,
	@BindParam("feature_ids[]") @NotEmpty @Size(max = 100) Set<@Positive Long> featureIds,
	@NotNull MultipartFile logo,
	@BindParam("main_image") @NotNull MultipartFile mainImage,
	@BindParam("interior_images[]") @NotEmpty @Size(max = 5) List<MultipartFile> interiorImages,
	@BindParam("business_registration_file") @NotNull MultipartFile businessRegistrationFile
) {

	public CreatePartnerCommand toCommand() {
		return new CreatePartnerCommand(
			name,
			description,
			industry,
			roadAddress,
			jibunAddress,
			detailAddress,
			latitude,
			longitude,
			operatingHoursNotice,
			operationHours,
			direction,
			allowStatus,
			status,
			contacts(),
			businessRegistration(),
			PartnerRequestSupport.ids(featureIds),
			MultipartMediaFileSource.from(logo),
			MultipartMediaFileSource.from(mainImage),
			interiorImages.stream().map(MultipartMediaFileSource::from).toList(),
			MultipartMediaFileSource.from(businessRegistrationFile)
		);
	}

	private PartnerContactSetCommand contacts() {
		return new PartnerContactSetCommand(
			representativePhone,
			smsSenderPhone,
			callReceiverPhone,
			PartnerRequestSupport.list(consultationReceiverPhones),
			PartnerRequestSupport.list(eventNoticeReceiverPhones),
			PartnerRequestSupport.list(noticeMarketingEmails)
		);
	}

	private PartnerBusinessRegistrationCommand businessRegistration() {
		return new PartnerBusinessRegistrationCommand(
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
