package com.platform.application.account.query;

import com.platform.domain.account.AccountPartnerStatus;
import java.util.List;

public record SearchPartnerAccountsForStaffQuery(
	String q,
	List<AccountPartnerStatus> status,
	Boolean dormant,
	String startDate,
	String endDate,
	String sort,
	String direction,
	int page,
	int perPage
) {
}
