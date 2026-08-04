package com.platform.application.partner.query;

import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.partner.PartnerRegistrationSource;
import java.util.List;

public record SearchPartnersQuery(
	String q,
	List<PartnerStatus> status,
	List<AccountPartnerStatus> accountStatus,
	List<PartnerAllowStatus> allowStatus,
	List<Long> categoryIds,
	List<PartnerRegistrationSource> registrationSources,
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
