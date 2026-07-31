package com.medi.application.partner.query;

import com.medi.domain.partner.PartnerAllowStatus;
import com.medi.domain.account.AccountPartnerStatus;
import com.medi.domain.partner.PartnerStatus;
import java.util.List;

public record SearchPartnersQuery(
	String q,
	List<PartnerStatus> status,
	List<AccountPartnerStatus> accountStatus,
	List<PartnerAllowStatus> allowStatus,
	List<Long> categoryIds,
	Boolean dormant,
	String startDate,
	String endDate,
	String updatedStartDate,
	String updatedEndDate,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
