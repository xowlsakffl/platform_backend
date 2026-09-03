package com.platform.application.partner.command;

import com.platform.application.media.storage.MediaFileSource;
import java.time.LocalDate;

public record CreateOwnedPartnerCommand(
	String name,
	String englishName,
	Long categoryId,
	String representativePhone,
	String representativeEmail,
	String businessNumber,
	String companyName,
	String ceoName,
	LocalDate openingDate,
	String roadAddress,
	String detailAddress,
	String latitude,
	String longitude,
	MediaFileSource businessRegistrationFile
) {
}
