package com.platform.application.partner.command;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.domain.partner.PartnerIndustry;
import java.util.List;
import java.util.Set;

public record UpdatePartnerOnboardingCommand(
	String name,
	String description,
	PartnerIndustry industry,
	String roadAddress,
	String jibunAddress,
	String detailAddress,
	String latitude,
	String longitude,
	String operatingHoursNotice,
	String operationHours,
	String direction,
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
