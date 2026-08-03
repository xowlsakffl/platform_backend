package com.platform.application.partner.command;

import java.util.List;

public record PartnerContactSetCommand(
	String representativePhone,
	String smsSenderPhone,
	String callReceiverPhone,
	List<String> consultationReceiverPhones,
	List<String> eventNoticeReceiverPhones,
	List<String> noticeMarketingEmails
) {
}
