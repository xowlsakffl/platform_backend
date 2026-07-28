package com.medi.application.hospital.command;

import java.util.List;

public record HospitalContactSetCommand(
	String representativePhone,
	String smsSenderPhone,
	String callReceiverPhone,
	List<String> consultationReceiverPhones,
	List<String> eventNoticeReceiverPhones,
	List<String> noticeMarketingEmails
) {
}
