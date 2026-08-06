package com.platform.application.partner;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PartnerBusinessNumberPolicy {

	private static final int BUSINESS_NUMBER_LENGTH = 10;

	public String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			throw invalid();
		}
		String trimmed = value.trim();
		if (!trimmed.matches("^(?:[0-9]{10}|[0-9]{3}-[0-9]{2}-[0-9]{5})$")) {
			throw invalid();
		}
		String normalized = trimmed.replace("-", "");
		if (normalized.length() != BUSINESS_NUMBER_LENGTH) {
			throw invalid();
		}
		return normalized;
	}

	private ApiException invalid() {
		return new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록번호 10자리를 입력해 주세요.");
	}
}
