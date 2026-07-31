package com.medi.application.media;

import com.medi.application.media.storage.StoredMediaFile;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.media.MediaOwnerType;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MediaCollectionPolicy {

	public static final String PARTNER_LOGO = "logo";
	public static final String PARTNER_MAIN_IMAGE = "main_image";
	public static final String PARTNER_INTERIOR_IMAGE = "interior_image";
	public static final String PARTNER_BUSINESS_REGISTRATION_FILE = "business_registration_file";
	public static final String CATEGORY_ICON = "icon";
	public static final String SPECIALIST_PROFILE_IMAGE = "profile_image";
	public static final String SPECIALIST_LICENSE_IMAGE = "license_image";
	public static final String SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE = "specialist_certificate_image";

	private static final long CATEGORY_ICON_MAX_SIZE = 5L * 1024 * 1024;
	private static final long PARTNER_LOGO_MAX_SIZE = 5L * 1024 * 1024;
	private static final long PARTNER_IMAGE_MAX_SIZE = 10L * 1024 * 1024;
	private static final long PARTNER_DOCUMENT_MAX_SIZE = 10L * 1024 * 1024;
	private static final long SPECIALIST_PROFILE_MAX_SIZE = 5L * 1024 * 1024;
	private static final long SPECIALIST_DOCUMENT_MAX_SIZE = 10L * 1024 * 1024;
	private static final Set<String> APP_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private static final Set<String> PARTNER_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private static final Set<String> DOCUMENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp",
		"application/pdf"
	);
	private static final Set<String> SPECIALIST_COLLECTIONS = Set.of(
		SPECIALIST_PROFILE_IMAGE,
		SPECIALIST_LICENSE_IMAGE,
		SPECIALIST_SPECIALIST_CERTIFICATE_IMAGE
	);

	public void validateCollection(MediaOwnerType ownerType, String collection) {
		boolean valid = switch (ownerType) {
			case PARTNER -> Set.of(PARTNER_LOGO, PARTNER_MAIN_IMAGE, PARTNER_INTERIOR_IMAGE).contains(collection);
			case PARTNER_BUSINESS_REGISTRATION -> PARTNER_BUSINESS_REGISTRATION_FILE.equals(collection);
			case CATEGORY -> CATEGORY_ICON.equals(collection);
			case SPECIALIST -> SPECIALIST_COLLECTIONS.contains(collection);
		};
		if (!valid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "해당 도메인에서 사용할 수 없는 미디어 컬렉션입니다.");
		}
	}

	public void validateFile(MediaOwnerType ownerType, String collection, StoredMediaFile file) {
		validateCollection(ownerType, collection);
		switch (ownerType) {
			case PARTNER -> validatePartnerFile(collection, file);
			case PARTNER_BUSINESS_REGISTRATION -> requireTypeAndSize(
				file,
				DOCUMENT_TYPES,
				PARTNER_DOCUMENT_MAX_SIZE,
				"사업자등록증은 JPG, PNG, WebP, PDF 10MB 이하만 가능합니다."
			);
			case CATEGORY -> validateCategoryIcon(file);
			case SPECIALIST -> validateSpecialistFile(collection, file);
		}
	}

	private void validatePartnerFile(String collection, StoredMediaFile file) {
		if (PARTNER_LOGO.equals(collection)) {
			requireTypeAndSize(file, APP_IMAGE_TYPES, PARTNER_LOGO_MAX_SIZE, "파트너 로고는 JPG, PNG, WebP 5MB 이하만 가능합니다.");
			if (file.width() == null || file.height() == null || !file.width().equals(file.height())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "파트너 로고는 1:1 비율이어야 합니다.");
			}
			return;
		}
		requireTypeAndSize(
			file,
			PARTNER_IMAGE_TYPES,
			PARTNER_IMAGE_MAX_SIZE,
			"파트너 대표/내부 이미지는 JPG, PNG 10MB 이하만 가능합니다."
		);
		if (file.width() == null || file.height() == null || file.width() != 760 || file.height() != 490) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "파트너 대표/내부 이미지는 760x490 크기여야 합니다.");
		}
	}

	private void validateCategoryIcon(StoredMediaFile file) {
		requireTypeAndSize(file, APP_IMAGE_TYPES, CATEGORY_ICON_MAX_SIZE, "카테고리 아이콘은 JPG, PNG, WebP 5MB 이하만 가능합니다.");
	}

	private void validateSpecialistFile(String collection, StoredMediaFile file) {
		if (SPECIALIST_PROFILE_IMAGE.equals(collection)) {
			requireTypeAndSize(
				file,
				APP_IMAGE_TYPES,
				SPECIALIST_PROFILE_MAX_SIZE,
				"스페셜리스트 프로필은 JPG, PNG, WebP 5MB 이하만 가능합니다."
			);
			if (file.width() == null || file.height() == null || !file.width().equals(file.height())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "스페셜리스트 프로필 이미지는 1:1 비율이어야 합니다.");
			}
			return;
		}
		requireTypeAndSize(
			file,
			DOCUMENT_TYPES,
			SPECIALIST_DOCUMENT_MAX_SIZE,
			"스페셜리스트 증빙 파일은 JPG, PNG, WebP, PDF 10MB 이하만 가능합니다."
		);
	}

	private void requireTypeAndSize(
		StoredMediaFile file,
		Set<String> allowedTypes,
		long maxSize,
		String message
	) {
		if (!allowedTypes.contains(file.mimeType()) || file.size() > maxSize) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
	}
}
