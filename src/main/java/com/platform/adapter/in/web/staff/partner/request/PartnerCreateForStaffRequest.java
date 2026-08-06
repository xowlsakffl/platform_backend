package com.platform.adapter.in.web.staff.partner.request;

import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.application.partner.command.CreatePartnerCommand;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record PartnerCreateForStaffRequest(
	@NotBlank @Size(max = 30) String name,
	@BindParam("english_name") @Size(max = 90) String englishName,
	@NotBlank @Size(max = 2000) String description,
	@BindParam("category_id") @NotNull @Positive Long categoryId,
	@BindParam("road_address") @NotBlank @Size(max = 255) String roadAddress,
	@BindParam("detail_address") @Size(max = 255) String detailAddress,
	@NotBlank @DecimalMin("-90") @DecimalMax("90") String latitude,
	@NotBlank @DecimalMin("-180") @DecimalMax("180") String longitude,
	@BindParam("subway_stations") @Size(max = 12000) String subwayStations,
	@BindParam("operating_hours_notice") @Size(max = 500) String operatingHoursNotice,
	@BindParam("operation_hours") @NotNull Object operationHours,
	@BindParam("holiday_policy") @NotNull Object holidayPolicy,
	@Size(max = 5000) String direction,
	@BindParam("representative_phone")
	@NotBlank
	@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN)
	String representativePhone,
	@BindParam("representative_email") @NotBlank @Email @Size(max = 255)
	String representativeEmail,
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
	@BindParam("business_number") @NotBlank
	@Pattern(regexp = PartnerRequestSupport.BUSINESS_NUMBER_PATTERN) String businessNumber,
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
	@BindParam("feature_ids[]") @NotEmpty @Size(max = 100) Set<@Positive Long> featureIds,
	@BindParam("hashtags[]") @Size(max = 10) List<@Size(max = 30) String> hashtags,
	@Size(max = 12000) String links,
	@NotNull MultipartFile logo,
	@BindParam("images[]") @NotEmpty @Size(max = 10) List<MultipartFile> images,
	@BindParam("business_registration_file") @NotNull MultipartFile businessRegistrationFile
) {

	public CreatePartnerCommand toCommand(List<PartnerOptionCreateForStaffRequest> options) {
		return new CreatePartnerCommand(
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
			contacts(),
			businessRegistration(),
			PartnerRequestSupport.ids(featureIds),
			PartnerRequestSupport.list(hashtags),
			options == null ? List.of() : options.stream().map(PartnerOptionCreateForStaffRequest::toCommand).toList(),
			links,
			MultipartMediaFileSource.from(logo),
			MultipartMediaFileSource.from(images.getFirst()),
			images.stream().skip(1).map(MultipartMediaFileSource::from).toList(),
			MultipartMediaFileSource.from(businessRegistrationFile)
		);
	}

	private PartnerContactSetCommand contacts() {
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
			settlementAccountHolder
		);
	}
}
