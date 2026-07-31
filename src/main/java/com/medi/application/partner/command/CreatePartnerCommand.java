package com.medi.application.partner.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.partner.PartnerAllowStatus;
import com.medi.domain.partner.PartnerStatus;
import java.util.Set;
import java.util.List;

public record CreatePartnerCommand(
	String name,
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
	MediaFileSource mainImage,
	List<MediaFileSource> interiorImages,
	MediaFileSource businessRegistrationFile
) {
}
