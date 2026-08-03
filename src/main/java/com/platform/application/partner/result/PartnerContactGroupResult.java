package com.platform.application.partner.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PartnerContactGroupResult(
	@JsonProperty("representative_phone") String representativePhone,
	@JsonProperty("sms_sender_phone") String smsSenderPhone,
	@JsonProperty("call_receiver_phone") String callReceiverPhone,
	@JsonProperty("consultation_receiver_phones") List<String> consultationReceiverPhones,
	@JsonProperty("event_notice_receiver_phones") List<String> eventNoticeReceiverPhones,
	@JsonProperty("notice_marketing_emails") List<String> noticeMarketingEmails
) {
}
