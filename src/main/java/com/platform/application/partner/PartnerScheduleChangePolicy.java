package com.platform.application.partner;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerScheduleChangePolicy {

	private final SpecialistRepository specialists;
	private final PartnerSchedulePolicyValidator validator;

	public PartnerScheduleChangePolicy(SpecialistRepository specialists, PartnerSchedulePolicyValidator validator) {
		this.specialists = specialists;
		this.validator = validator;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void assertCompatible(Long partnerId, String operationHours) {
		var conflicts = new ArrayList<String>();
		for (var specialist : specialists.findByPartner_IdAndDeletedAtIsNull(partnerId)) {
			if (specialist.scheduleMode() != SpecialistScheduleMode.CUSTOM_HOURS) {
				continue;
			}
			try {
				validator.assertWithinPartnerHours(specialist.operationHours(), operationHours);
			} catch (ApiException exception) {
				conflicts.add(specialist.name() + " (#" + specialist.id() + ")");
			}
		}
		if (!conflicts.isEmpty()) {
			String message = "업체 운영시간을 벗어나는 전문가가 있습니다: " + String.join(", ", conflicts)
				+ ". 해당 전문가의 개별 근무시간을 먼저 변경해 주세요.";
			throw new ApiException(ErrorCode.INVALID_REQUEST, message, Map.of("operation_hours", message));
		}
	}
}
