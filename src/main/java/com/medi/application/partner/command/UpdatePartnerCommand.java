package com.medi.application.partner.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.partner.PartnerAllowStatus;
import com.medi.domain.partner.PartnerStatus;
import java.util.Set;
import java.util.List;

public record UpdatePartnerCommand(
	String description,
	String instagramLink,
	String kakaoLink,
	String roadAddress,
	String jibunAddress,
	String latitude,
	String longitude,
	String operatingHoursNotice,
	Object operationHours,
	String direction,
	PartnerAllowStatus allowStatus,
	PartnerStatus status,
	PartnerContactSetCommand contacts,
	PartnerBusinessRegistrationCommand businessRegistration,
	Set<Long> categoryIds,
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
