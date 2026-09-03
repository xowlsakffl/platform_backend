package com.platform.application.notice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.auth.PermissionService;
import com.platform.application.auth.command.AuthClientContext;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaLifecycleService;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.notice.command.SaveNoticeCommand;
import com.platform.application.notice.query.SearchNoticesQuery;
import com.platform.application.notice.result.NoticeFileResult;
import com.platform.application.notice.result.NoticeHistoryResult;
import com.platform.application.notice.result.NoticeResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.notice.Notice;
import com.platform.domain.notice.NoticePublicationStatus;
import com.platform.domain.notice.NoticeStatus;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.infrastructure.persistence.notice.NoticeRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {
	private final NoticeRepository notices;
	private final PermissionService permissions;
	private final NoticeContentPolicy contentPolicy;
	private final MediaCommandService mediaCommands;
	private final MediaReadService media;
	private final MediaLifecycleService lifecycle;
	private final NoticeMediaService noticeMedia;
	private final OperationHistoryRepository histories;
	private final ObjectMapper json;

	public NoticeService(NoticeRepository notices, PermissionService permissions, NoticeContentPolicy contentPolicy,
		MediaCommandService mediaCommands, MediaReadService media, MediaLifecycleService lifecycle,
		NoticeMediaService noticeMedia, OperationHistoryRepository histories, ObjectMapper json) {
		this.notices = notices;
		this.permissions = permissions;
		this.contentPolicy = contentPolicy;
		this.mediaCommands = mediaCommands;
		this.media = media;
		this.lifecycle = lifecycle;
		this.noticeMedia = noticeMedia;
		this.histories = histories;
		this.json = json;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<NoticeResult> list(AuthenticatedActor actor, SearchNoticesQuery query) {
		boolean staff = requireRead(actor);
		var now = NoticeMediaService.now();
		var page = notices.findAll(search(query, staff, now), PageRequest.of(query.page() - 1, query.perPage(), order()));
		return PaginatedResponse.from(page, notice -> result(notice, staff, false, now));
	}

	@Transactional(readOnly = true)
	public NoticeResult get(AuthenticatedActor actor, Long id) {
		boolean staff = requireRead(actor);
		Notice notice = find(id);
		var now = NoticeMediaService.now();
		if (!staff && notice.publicationStatus(now) != NoticePublicationStatus.PUBLISHED) throw notFound();
		return result(notice, staff, true, now);
	}

	@Transactional(readOnly = true)
	public List<NoticeResult> popups(AuthenticatedActor actor) {
		permissions.requireActor(actor, AccountActorType.PARTNER);
		var query = new SearchNoticesQuery(null, null, true, null, null, 1, 50);
		var now = NoticeMediaService.now();
		var items = notices.findAll(search(query, false, now), PageRequest.of(0, 50, order())).getContent();
		var ownerIds = items.stream().map(Notice::id).collect(java.util.stream.Collectors.toSet());
		var attachments = media.listByOwners(MediaOwnerType.NOTICE, ownerIds, MediaCollectionPolicy.NOTICE_ATTACHMENT);
		return items.stream().map(notice -> result(notice, false, true, now,
			attachments.getOrDefault(notice.id(), List.of()))).toList();
	}

	@Transactional
	public NoticeResult save(AuthenticatedActor actor, Long id, SaveNoticeCommand command, List<MediaFileSource> files, AuthClientContext client) {
		permissions.requireStaffPermission(actor, id == null ? AccessPermissions.NOTICE_CREATE : AccessPermissions.NOTICE_UPDATE);
		if (files != null) for (MediaFileSource file : files) {
			if (file == null || file.size() <= 0) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "첨부파일은 빈 파일을 등록할 수 없습니다.");
			}
			String name = file.originalFilename();
			if (name == null || !name.toLowerCase(java.util.Locale.ROOT).matches(".+\\.(jpg|jpeg|png|webp|pdf|docx|xlsx|pptx)")) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "허용되지 않는 첨부파일 확장자입니다.");
			}
		}
		var now = NoticeMediaService.now();
		if ((command.publishStartAt() == null) != (command.publishEndAt() == null)
			|| (command.publishStartAt() != null && !command.publishStartAt().isBefore(command.publishEndAt()))) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "게시 시작일과 종료일을 확인해 주세요. 종료일은 시작일보다 늦어야 합니다.");
		}
		Notice notice = id == null ? new Notice(actor.accountId(), actor.name())
			: notices.findLockedByIdAndDeletedAtIsNull(id).orElseThrow(this::notFound);
		if (id != null && !Objects.equals(command.version(), notice.version())) throw new ApiException(ErrorCode.CONFLICT);
		Map<String, String> before = id == null ? Map.of() : snapshot(notice);
		var clean = contentPolicy.clean(command.content());
		notice.update(command.title().trim(), clean.html(), clean.plainText(), command.status(),
			command.publishStartAt(), command.publishEndAt(), command.pinned(), command.popup(), now);
		notices.saveAndFlush(notice);
		noticeMedia.assignImages(actor, notice.id(), clean.imageIds());
		mediaCommands.synchronizeMany(MediaOwnerType.NOTICE, notice.id(), MediaCollectionPolicy.NOTICE_ATTACHMENT,
			files, command.attachmentIds(), false, 5);
		record(actor, notice, id == null ? "CREATED" : "UPDATED", before, snapshot(notice), client);
		return result(notice, true, true, now);
	}

	@Transactional
	public void delete(AuthenticatedActor actor, Long id, AuthClientContext client) {
		permissions.requireStaffPermission(actor, AccessPermissions.NOTICE_DELETE);
		Notice notice = notices.findLockedByIdAndDeletedAtIsNull(id).orElseThrow(this::notFound);
		notice.softDelete(NoticeMediaService.now());
		lifecycle.softDeleteOwnedMedia(MediaOwnerType.NOTICE, id);
		record(actor, notice, "DELETED", Map.of("title", notice.title()), Map.of(), client);
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<NoticeHistoryResult> histories(AuthenticatedActor actor, Long id, int page, int size) {
		permissions.requireStaffPermission(actor, AccessPermissions.NOTICE_SHOW);
		find(id);
		var rows = histories.findByTargetTypeAndTargetId(OperationHistory.TARGET_NOTICE, id,
			PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
		Map<Long, OperationHistory> expanded = new LinkedHashMap<>();
		if (!rows.isEmpty()) histories.findWithChangesByIdIn(rows.map(OperationHistory::id).toList())
			.forEach(item -> expanded.put(item.id(), item));
		return PaginatedResponse.from(rows, row -> {
			OperationHistory item = expanded.get(row.id());
			return new NoticeHistoryResult(item.id(), item.action(), item.targetType(), item.targetId(),
				item.actorType(), item.actorId(), item.actorNameSnapshot(), item.actorLoginIdSnapshot(),
				item.ipAddress(), item.createdAt(),
				item.changes().stream().map(change -> new NoticeHistoryResult.Change(change.fieldKey(), change.beforeValue(), change.afterValue())).toList());
		});
	}

	private Specification<Notice> search(SearchNoticesQuery query, boolean staff, LocalDateTime now) {
		return (root, criteria, cb) -> {
			var predicates = new ArrayList<Predicate>();
			predicates.add(cb.isNull(root.get("deletedAt")));
			NoticePublicationStatus state = staff ? query.publicationStatus() : NoticePublicationStatus.PUBLISHED;
			if (state != null) {
				predicates.add(cb.equal(root.get("status"), state == NoticePublicationStatus.HIDDEN ? NoticeStatus.PRIVATE : NoticeStatus.PUBLIC));
				switch (state) {
					case SCHEDULED -> predicates.add(cb.greaterThan(root.get("publishStartAt"), now));
					case ENDED -> predicates.add(cb.lessThanOrEqualTo(root.get("publishEndAt"), now));
					case PUBLISHED -> {
						predicates.add(cb.or(cb.isNull(root.get("publishStartAt")), cb.lessThanOrEqualTo(root.get("publishStartAt"), now)));
						predicates.add(cb.or(cb.isNull(root.get("publishEndAt")), cb.greaterThan(root.get("publishEndAt"), now)));
					}
					default -> { }
				}
			}
			if (query.popup() != null) predicates.add(cb.equal(root.get("popup"), query.popup()));
			if (query.search() != null && !query.search().isBlank()) {
				String term = "%" + query.search().trim().toLowerCase(java.util.Locale.ROOT)
					.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("title")), term, '\\'), cb.like(cb.lower(root.get("plainContent")), term, '\\')));
			}
			if (query.createdFrom() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom().atStartOfDay()));
			if (query.createdTo() != null) predicates.add(cb.lessThan(root.get("createdAt"), query.createdTo().plusDays(1).atStartOfDay()));
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private NoticeResult result(Notice notice, boolean staff, boolean detail, LocalDateTime now) {
		var files = detail ? media.list(MediaOwnerType.NOTICE, notice.id(), MediaCollectionPolicy.NOTICE_ATTACHMENT) : List.<MediaResult>of();
		return result(notice, staff, detail, now, files);
	}

	private NoticeResult result(Notice notice, boolean staff, boolean detail, LocalDateTime now, List<MediaResult> files) {
		var attachments = files.stream()
			.map(item -> new NoticeFileResult(item.id(), item.originalName(), item.mimeType(), item.size(),
				staff ? "/api/v1/staff/notices/media/" + item.id() + "/content"
					: "/api/v1/partner/notices/" + notice.id() + "/media/" + item.id() + "/content")).toList();
		return new NoticeResult(notice.id(), notice.version(), notice.title(), detail ? notice.content() : null,
			notice.status(), notice.publicationStatus(now), notice.publishStartAt(), notice.publishEndAt(), notice.pinned(),
			notice.popup(), staff ? notice.authorName() : null, notice.createdAt(), notice.updatedAt(), attachments);
	}

	private Map<String, String> snapshot(Notice notice) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("title", notice.title()); values.put("content", notice.content());
		values.put("status", notice.status().name()); values.put("pinned", Boolean.toString(notice.pinned()));
		values.put("popup", Boolean.toString(notice.popup()));
		values.put("publish_start_at", notice.publishStartAt() == null ? null : notice.publishStartAt().toString());
		values.put("publish_end_at", notice.publishEndAt() == null ? null : notice.publishEndAt().toString());
		try {
			values.put("attachments", json.writeValueAsString(media.list(MediaOwnerType.NOTICE, notice.id(), MediaCollectionPolicy.NOTICE_ATTACHMENT)
				.stream().map(file -> Map.of("id", file.id(), "name", file.originalName())).toList()));
		} catch (JsonProcessingException exception) { throw new InternalApplicationException("공지 이력을 만들 수 없습니다.", exception); }
		return values;
	}

	private void record(AuthenticatedActor actor, Notice notice, String action, Map<String, String> before, Map<String, String> after, AuthClientContext client) {
		var history = new OperationHistory(OperationHistory.TARGET_NOTICE, notice.id(), actor.actorType().name(), actor.accountId(), action, null, null)
			.captureActor(actor.name(), actor.loginId())
			.captureRequest(client.ipAddress(), client.userAgent());
		var keys = new LinkedHashSet<>(before.keySet()); keys.addAll(after.keySet());
		for (String key : keys) if (!Objects.equals(before.get(key), after.get(key))) history.addChange(key, before.get(key), after.get(key));
		if (!history.changes().isEmpty()) histories.save(history);
	}
	private Sort order() { return Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")); }
	private boolean requireRead(AuthenticatedActor actor) {
		if (actor != null && actor.actorType() == AccountActorType.STAFF) {
			permissions.requireStaffPermission(actor, AccessPermissions.NOTICE_SHOW); return true;
		}
		permissions.requireActor(actor, AccountActorType.PARTNER); return false;
	}
	private Notice find(Long id) { return notices.findByIdAndDeletedAtIsNull(id).orElseThrow(this::notFound); }
	private ApiException notFound() { return new ApiException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다."); }
}
