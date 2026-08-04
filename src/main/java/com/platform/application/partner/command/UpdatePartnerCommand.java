package com.platform.application.partner.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import java.util.Set;
import java.util.List;

public record UpdatePartnerCommand(
	String accountInvitationEmail,
	String description,
	Long categoryId,
	String roadAddress,
	String jibunAddress,
	String detailAddress,
	String latitude,
	String longitude,
	String operatingHoursNotice,
	Object operationHours,
	String direction,
	PartnerAllowStatus allowStatus,
	PartnerStatus status,
	PartnerContactSetCommand contacts,
	PartnerBusinessRegistrationCommand businessRegistration,
	Set<Long> featureIds,
	MediaFileSource logo,
	Long existingLogoId,
	MediaFileSource mainImage,
	Long existingMainImageId,
	List<MediaFileSource> interiorImages,
	List<Long> existingInteriorImageIds,
	List<String> interiorImageOrder,
	MediaFileSource businessRegistrationFile,
	Long existingBusinessRegistrationFileId,
	Set<String> specifiedFields
) {

	public boolean specified(String field) {
		return specifiedFields.contains(field);
	}
}
