package com.medi.adapter.in.web.staff.hospital.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medi.application.hospital.command.HospitalBusinessRegistrationCommand;
import com.medi.application.hospital.command.HospitalContactSetCommand;
import com.medi.application.hospital.command.UpdateHospitalCommand;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalDepartment;
import com.medi.domain.hospital.HospitalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record HospitalUpdateRequest(
	HospitalDepartment department,
	@Size(max = 5000) String description,
	@JsonProperty("youtube_link") @Size(max = 500) String youtubeLink,
	@Size(max = 255) String address,
	@JsonProperty("address_detail") @Size(max = 255) String addressDetail,
	String latitude,
	String longitude,
	@JsonProperty("consulting_hours") @Size(max = 5000) String consultingHours,
	@JsonProperty("operation_hours") Object operationHours,
	@Size(max = 5000) String direction,
	@JsonProperty("allow_status") HospitalAllowStatus allowStatus,
	HospitalStatus status,
	@JsonProperty("representative_phone")
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String representativePhone,
	@JsonProperty("sms_sender_phone")
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String smsSenderPhone,
	@JsonProperty("call_receiver_phone")
	@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN)
	String callReceiverPhone,
	@JsonProperty("consultation_receiver_phones")
	@Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> consultationReceiverPhones,
	@JsonProperty("event_notice_receiver_phones")
	@Size(max = 3)
	List<@Pattern(regexp = HospitalRequestSupport.PHONE_PATTERN) String> eventNoticeReceiverPhones,
	@JsonProperty("notice_marketing_emails") @Size(max = 3) List<@Email String> noticeMarketingEmails,
	@JsonProperty("business_number") @Size(max = 20) String businessNumber,
	@JsonProperty("company_name") @Size(max = 255) String companyName,
	@JsonProperty("ceo_name") @Size(max = 100) String ceoName,
	@JsonProperty("business_type") @Size(max = 100) String businessType,
	@JsonProperty("business_item") @Size(max = 100) String businessItem,
	@JsonProperty("business_address") @Size(max = 255) String businessAddress,
	@JsonProperty("business_address_detail") @Size(max = 255) String businessAddressDetail,
	@JsonProperty("settlement_bank_name") @Size(max = 50) String settlementBankName,
	@JsonProperty("settlement_account_number") @Size(max = 50) String settlementAccountNumber,
	@JsonProperty("settlement_account_holder") @Size(max = 100) String settlementAccountHolder,
	@JsonProperty("tax_invoice_email") @Email String taxInvoiceEmail,
	@JsonProperty("issued_at") LocalDate issuedAt,
	@JsonProperty("category_ids") @Size(max = 5) Set<Long> categoryIds,
	@JsonProperty("feature_ids") @Size(max = 100) Set<Long> featureIds
) {

	public UpdateHospitalCommand toCommand() {
		return new UpdateHospitalCommand(
			department,
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
			contactsOrNull(),
			businessRegistrationOrNull(),
			categoryIds == null ? null : HospitalRequestSupport.ids(categoryIds),
			featureIds == null ? null : HospitalRequestSupport.ids(featureIds)
		);
	}

	private HospitalContactSetCommand contactsOrNull() {
		if (representativePhone == null
			&& smsSenderPhone == null
			&& callReceiverPhone == null
			&& consultationReceiverPhones == null
			&& eventNoticeReceiverPhones == null
			&& noticeMarketingEmails == null) {
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

	private HospitalBusinessRegistrationCommand businessRegistrationOrNull() {
		if (businessNumber == null
			&& companyName == null
			&& ceoName == null
			&& businessType == null
			&& businessItem == null
			&& businessAddress == null
			&& businessAddressDetail == null
			&& settlementBankName == null
			&& settlementAccountNumber == null
			&& settlementAccountHolder == null
			&& taxInvoiceEmail == null
			&& issuedAt == null) {
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
}
