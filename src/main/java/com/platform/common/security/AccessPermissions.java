package com.platform.common.security;

public final class AccessPermissions {

	public static final String COMMON_ACCESS = "common.access";
	public static final String COMMON_DASHBOARD_SHOW = "common.dashboard.show";
	public static final String COMMON_PROFILE_SHOW = "common.profile.show";
	public static final String COMMON_PROFILE_UPDATE = "common.profile.update";

	public static final String PARTNER_SHOW = "platform.partner.show";
	public static final String PARTNER_CREATE = "platform.partner.create";
	public static final String PARTNER_UPDATE = "platform.partner.update";
	public static final String PARTNER_DELETE = "platform.partner.delete";
	public static final String PARTNER_ASSIGN_STAFF = "platform.partner.assign_staff";
	public static final String PARTNER_ACCOUNT_STATUS_UPDATE = "platform.partner.account_status.update";
	public static final String PARTNER_ALLOW_STATUS_UPDATE = "platform.partner.allow_status.update";
	public static final String PARTNER_STATUS_UPDATE = "platform.partner.status.update";

	public static final String SPECIALIST_SHOW = "platform.specialist.show";
	public static final String SPECIALIST_CREATE = "platform.specialist.create";
	public static final String SPECIALIST_UPDATE = "platform.specialist.update";
	public static final String SPECIALIST_DELETE = "platform.specialist.delete";

	public static final String CATEGORY_MANAGE = "platform.category.manage";

	private AccessPermissions() {
	}
}
