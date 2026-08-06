package com.platform.application.partner.command;

import com.platform.application.media.storage.MediaFileSource;
import java.util.Set;
import java.util.List;

public record CreatePartnerCommand(
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
	PartnerContactSetCommand contacts,
	PartnerBusinessRegistrationCommand businessRegistration,
	Set<Long> featureIds,
	List<String> hashtags,
	List<SavePartnerOptionCommand> options,
	String linksJson,
	MediaFileSource logo,
	MediaFileSource mainImage,
	List<MediaFileSource> interiorImages,
	MediaFileSource businessRegistrationFile
) {
}
