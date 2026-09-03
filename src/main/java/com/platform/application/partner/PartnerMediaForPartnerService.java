package com.platform.application.partner;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.result.MediaContentResult;
import com.platform.application.media.storage.MediaContent;
import com.platform.application.media.storage.MediaStorage;
import com.platform.application.media.storage.MediaStorageException;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.media.Media;
import com.platform.domain.partner.Partner;
import com.platform.infrastructure.persistence.media.MediaRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerMediaForPartnerService {

	private static final Set<String> PARTNER_COLLECTIONS = Set.of(
		MediaCollectionPolicy.PARTNER_LOGO,
		MediaCollectionPolicy.PARTNER_MAIN_IMAGE,
		MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE
	);

	private final OwnershipPolicy ownershipPolicy;
	private final PartnerRepository partnerRepository;
	private final MediaRepository mediaRepository;
	private final MediaStorage mediaStorage;

	public PartnerMediaForPartnerService(
		OwnershipPolicy ownershipPolicy,
		PartnerRepository partnerRepository,
		MediaRepository mediaRepository,
		MediaStorage mediaStorage
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.partnerRepository = partnerRepository;
		this.mediaRepository = mediaRepository;
		this.mediaStorage = mediaStorage;
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(AuthenticatedActor actor, Long partnerId, Long mediaId) {
		ownershipPolicy.requirePartnerOwner(actor, partnerId);
		Partner partner = partnerRepository.findByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업체를 찾을 수 없습니다."));
		Media media = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
			.filter(item -> belongsToPartner(item, partner))
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "미디어를 찾을 수 없습니다."));
		try {
			MediaContent content = mediaStorage.load(media.path());
			return new MediaContentResult(media.originalName(), media.mimeType(), content.contentLength(), content);
		} catch (MediaStorageException exception) {
			throw switch (exception.reason()) {
				case INVALID_FILE, FILE_TOO_LARGE -> new ApiException(ErrorCode.INVALID_REQUEST, exception.getMessage());
				case FILE_NOT_FOUND -> new ApiException(ErrorCode.NOT_FOUND, exception.getMessage());
				case IO_ERROR -> new InternalApplicationException("미디어 파일 조회 중 오류가 발생했습니다.", exception);
			};
		}
	}

	private boolean belongsToPartner(Media media, Partner partner) {
		return switch (media.ownerType()) {
			case PARTNER -> partner.id().equals(media.ownerId()) && PARTNER_COLLECTIONS.contains(media.collection());
			case PARTNER_BUSINESS_REGISTRATION -> partner.businessRegistration() != null
				&& partner.businessRegistration().id().equals(media.ownerId())
				&& MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE.equals(media.collection());
			case CATEGORY, SPECIALIST -> false;
		};
	}
}
