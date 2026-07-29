package com.medi.common.security;

public final class AccessPermissions {

	public static final String COMMON_ACCESS = "common.access";
	public static final String COMMON_DASHBOARD_SHOW = "common.dashboard.show";
	public static final String COMMON_PROFILE_SHOW = "common.profile.show";
	public static final String COMMON_PROFILE_UPDATE = "common.profile.update";

	public static final String HOSPITAL_SHOW = "platform.hospital.show";
	public static final String HOSPITAL_CREATE = "platform.hospital.create";
	public static final String HOSPITAL_UPDATE = "platform.hospital.update";
	public static final String HOSPITAL_DELETE = "platform.hospital.delete";

	public static final String DOCTOR_SHOW = "platform.doctor.show";
	public static final String DOCTOR_CREATE = "platform.doctor.create";
	public static final String DOCTOR_UPDATE = "platform.doctor.update";
	public static final String DOCTOR_DELETE = "platform.doctor.delete";

	public static final String CATEGORY_MANAGE = "platform.category.manage";

	private AccessPermissions() {
	}
}
