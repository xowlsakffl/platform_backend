package com.medi.application.hospital.command;

import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalInterpretationLanguage;
import com.medi.domain.hospital.HospitalStatus;
import java.util.Set;
import java.util.List;

public record UpdateHospitalCommand(
	String description,
	String youtubeLink,
	String address,
	String addressDetail,
	String latitude,
	String longitude,
	String consultingHours,
	Object operationHours,
	String direction,
	HospitalAllowStatus allowStatus,
	HospitalStatus status,
	HospitalContactSetCommand contacts,
	HospitalBusinessRegistrationCommand businessRegistration,
	Set<Long> categoryIds,
	Set<Long> featureIds,
	Set<HospitalInterpretationLanguage> interpretationLanguages,
	MediaFileSource logo,
	Long existingLogoId,
	List<MediaFileSource> gallery,
	List<Long> existingGalleryIds,
	List<String> galleryOrder,
	MediaFileSource businessRegistrationFile,
	Long existingBusinessRegistrationFileId,
	Set<String> specifiedFields
) {

	public boolean specified(String field) {
		return specifiedFields.contains(field);
	}
}
