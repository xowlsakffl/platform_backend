package com.medi.application.hospital.result;

import java.util.List;

public record HospitalContactGroupResult(
	String representativePhone,
	String smsSenderPhone,
	String callReceiverPhone,
	List<String> consultationReceiverPhones,
	List<String> eventNoticeReceiverPhones,
	List<String> noticeMarketingEmails
) {
}
