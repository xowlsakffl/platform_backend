package com.platform.application.partner.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerStatus;
import java.util.List;
import java.util.Set;

public record UpdatePartnerCommand(
	String name,
	String englishName,
	String description,
	Long categoryId,
	String roadAddress,
	String detailAddress,
	String latitude,
	String longitude,
	String subwayStationsJson,
	String operatingHoursNotice,
	Object operationHours,
	Object holidayPolicy,
	String direction,
	PartnerAllowStatus allowStatus,
	PartnerStatus status,
	PartnerContactSetCommand contacts,
	PartnerBusinessRegistrationCommand businessRegistration,
	Set<Long> featureIds,
	List<String> hashtags,
	String linksJson,
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
