package com.platform.application.partner.query;

import com.platform.domain.partner.PartnerAccountInvitationStatus;
import java.util.List;

public record SearchPartnerAccountInvitationsQuery(
	String q,
	Long partnerId,
	List<PartnerAccountInvitationStatus> status,
	String startDate,
	String endDate,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
