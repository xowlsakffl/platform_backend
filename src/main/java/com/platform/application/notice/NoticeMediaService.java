package com.platform.application.notice;

import com.platform.application.auth.PermissionService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaLifecycleService;
import com.platform.application.media.result.MediaContentResult;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.media.storage.MediaStorage;
import com.platform.application.media.storage.MediaStorageException;
import com.platform.application.notice.result.NoticeFileResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.media.Media;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.notice.NoticePublicationStatus;
import com.platform.infrastructure.persistence.media.MediaRepository;
import com.platform.infrastructure.persistence.notice.NoticeRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeMediaService {
	private final PermissionService permissions;
	private final MediaRepository media;
	private final NoticeRepository notices;
	private final MediaCommandService commands;
	private final MediaStorage storage;
	private final MediaLifecycleService cleanup;

	public NoticeMediaService(PermissionService permissions, MediaRepository media, NoticeRepository notices,
		MediaCommandService commands, MediaStorage storage, MediaLifecycleService cleanup) {
		this.permissions = permissions;
		this.media = media;
		this.notices = notices;
		this.commands = commands;
		this.storage = storage;
		this.cleanup = cleanup;
	}

	@Transactional
	public NoticeFileResult upload(AuthenticatedActor actor, MediaFileSource file) {
		requireEditor(actor);
		var result = commands.append(MediaOwnerType.NOTICE_TEMP, actor.accountId(), MediaCollectionPolicy.NOTICE_EDITOR_IMAGE, file);
		return new NoticeFileResult(result.id(), result.originalName(), result.mimeType(), result.size(), "/notice-media/" + result.id());
	}

	@Transactional
	public void removeTemporary(AuthenticatedActor actor, List<Long> ids) {
		requireEditor(actor);
		for (Long id : ids.stream().distinct().sorted().toList()) {
			Media item = media.findLockedByIdAndDeletedAtIsNull(id).orElse(null);
			if (item == null) continue;
			if (item.ownerType() != MediaOwnerType.NOTICE_TEMP || !item.ownerId().equals(actor.accountId())) continue;
			delete(item);
		}
	}

	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
	public void assignImages(AuthenticatedActor actor, Long noticeId, List<Long> imageIds) {
		for (Long id : imageIds.stream().sorted().toList()) {
			Media item = media.findLockedByIdAndDeletedAtIsNull(id).orElseThrow(this::notFound);
			boolean temporary = item.ownerType() == MediaOwnerType.NOTICE_TEMP && item.ownerId().equals(actor.accountId());
			boolean owned = item.ownerType() == MediaOwnerType.NOTICE && item.ownerId().equals(noticeId);
			if ((!temporary && !owned) || !MediaCollectionPolicy.NOTICE_EDITOR_IMAGE.equals(item.collection())) throw notFound();
			item.assignOwner(MediaOwnerType.NOTICE, noticeId);
		}
		for (Media item : media.findByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
			MediaOwnerType.NOTICE, noticeId, MediaCollectionPolicy.NOTICE_EDITOR_IMAGE)) {
			if (!imageIds.contains(item.id())) delete(item);
		}
	}

	@Transactional(readOnly = true)
	public MediaContentResult content(AuthenticatedActor actor, Long noticeId, Long mediaId) {
		Media item = media.findByIdAndDeletedAtIsNull(mediaId).orElseThrow(this::notFound);
		if (actor.actorType() == AccountActorType.STAFF) {
			permissions.requireStaffPermission(actor, AccessPermissions.NOTICE_SHOW);
			if (item.ownerType() == MediaOwnerType.NOTICE_TEMP) {
				if (!item.ownerId().equals(actor.accountId())) throw notFound();
			} else {
				if (item.ownerType() != MediaOwnerType.NOTICE) throw notFound();
				notices.findByIdAndDeletedAtIsNull(item.ownerId()).orElseThrow(this::notFound);
			}
		} else {
			permissions.requireActor(actor, AccountActorType.PARTNER);
			if (item.ownerType() != MediaOwnerType.NOTICE || !Objects.equals(noticeId, item.ownerId())) throw notFound();
			var notice = notices.findByIdAndDeletedAtIsNull(noticeId).orElseThrow(this::notFound);
			if (notice.publicationStatus(now()) != NoticePublicationStatus.PUBLISHED) throw notFound();
		}
		try {
			var content = storage.load(item.path());
			return new MediaContentResult(item.originalName(), item.mimeType(), content.contentLength(), content);
		} catch (MediaStorageException exception) {
			if (exception.reason() == MediaStorageException.Reason.FILE_NOT_FOUND) throw notFound();
			throw new InternalApplicationException("공지 파일 조회에 실패했습니다.", exception);
		}
	}

	@Scheduled(fixedDelay = 3_600_000)
	@Transactional
	public void cleanupExpiredImages() {
		var cutoff = LocalDateTime.now().minusHours(24);
		// Read only IDs first so the locked read sees the latest owner after a concurrent save.
		for (Long id : media.findExpiredTemporaryIds(MediaOwnerType.NOTICE_TEMP, cutoff, org.springframework.data.domain.PageRequest.of(0, 100))) {
			var item = media.findLockedByIdAndDeletedAtIsNull(id).orElse(null);
			if (item != null && item.ownerType() == MediaOwnerType.NOTICE_TEMP && item.createdAt().isBefore(cutoff)) delete(item);
		}
	}

	private void requireEditor(AuthenticatedActor actor) {
		permissions.requireStaffPermission(actor, AccessPermissions.NOTICE_SHOW);
		if (!permissions.hasStaffPermission(actor, AccessPermissions.NOTICE_CREATE)
			&& !permissions.hasStaffPermission(actor, AccessPermissions.NOTICE_UPDATE)) throw new ApiException(ErrorCode.FORBIDDEN);
	}
	private void delete(Media item) { cleanup.softDelete(item); }
	private ApiException notFound() { return new ApiException(ErrorCode.NOT_FOUND, "공지 파일을 찾을 수 없습니다."); }
	public static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Seoul")); }
}
