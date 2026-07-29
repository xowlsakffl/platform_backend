package com.medi.application.media;

import com.medi.application.media.storage.StoredMediaFile;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.domain.media.MediaOwnerType;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MediaCollectionPolicy {

	public static final String CATEGORY_ICON = "icon";
	public static final String DOCTOR_PROFILE_IMAGE = "profile_image";
	public static final String DOCTOR_LICENSE_IMAGE = "license_image";
	public static final String DOCTOR_SPECIALIST_CERTIFICATE_IMAGE = "specialist_certificate_image";

	private static final long CATEGORY_ICON_MAX_SIZE = 5L * 1024 * 1024;
	private static final long DOCTOR_PROFILE_MAX_SIZE = 5L * 1024 * 1024;
	private static final long DOCTOR_DOCUMENT_MAX_SIZE = 10L * 1024 * 1024;
	private static final Set<String> APP_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private static final Set<String> DOCUMENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp",
		"application/pdf"
	);
	private static final Set<String> DOCTOR_COLLECTIONS = Set.of(
		DOCTOR_PROFILE_IMAGE,
		DOCTOR_LICENSE_IMAGE,
		DOCTOR_SPECIALIST_CERTIFICATE_IMAGE
	);

	public void validateCollection(MediaOwnerType ownerType, String collection) {
		boolean valid = switch (ownerType) {
			case HOSPITAL -> true;
			case CATEGORY -> CATEGORY_ICON.equals(collection);
			case DOCTOR -> DOCTOR_COLLECTIONS.contains(collection);
		};
		if (!valid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "해당 도메인에서 사용할 수 없는 미디어 컬렉션입니다.");
		}
	}

	public void validateFile(MediaOwnerType ownerType, String collection, StoredMediaFile file) {
		validateCollection(ownerType, collection);
		switch (ownerType) {
			case HOSPITAL -> {
				return;
			}
			case CATEGORY -> validateCategoryIcon(file);
			case DOCTOR -> validateDoctorFile(collection, file);
		}
	}

	private void validateCategoryIcon(StoredMediaFile file) {
		requireTypeAndSize(file, APP_IMAGE_TYPES, CATEGORY_ICON_MAX_SIZE, "카테고리 아이콘은 JPG, PNG, WebP 5MB 이하만 가능합니다.");
	}

	private void validateDoctorFile(String collection, StoredMediaFile file) {
		if (DOCTOR_PROFILE_IMAGE.equals(collection)) {
			requireTypeAndSize(
				file,
				APP_IMAGE_TYPES,
				DOCTOR_PROFILE_MAX_SIZE,
				"의료진 프로필은 JPG, PNG, WebP 5MB 이하만 가능합니다."
			);
			if (file.width() == null || file.height() == null || !file.width().equals(file.height())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "의료진 프로필 이미지는 1:1 비율이어야 합니다.");
			}
			return;
		}
		requireTypeAndSize(
			file,
			DOCUMENT_TYPES,
			DOCTOR_DOCUMENT_MAX_SIZE,
			"의료진 증빙 파일은 JPG, PNG, WebP, PDF 10MB 이하만 가능합니다."
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
