package com.platform.adapter.in.web.staff.notice.controller;

import com.platform.adapter.in.web.staff.notice.request.NoticeImageCleanupRequest;
import com.platform.adapter.in.web.staff.notice.request.NoticeSaveRequest;
import com.platform.application.notice.NoticeMediaService;
import com.platform.application.notice.NoticeService;
import com.platform.application.media.storage.MediaFileSource;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import com.platform.common.web.auth.AuthRequestSupport;
import com.platform.common.web.multipart.MultipartMediaFileSource;
import com.platform.common.web.notice.NoticeFileResponse;
import com.platform.common.web.notice.NoticeListRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/staff/notices")
public class NoticeForStaffController {
	private final NoticeService service;
	private final NoticeMediaService media;
	public NoticeForStaffController(NoticeService service, NoticeMediaService media) { this.service = service; this.media = media; }

	@GetMapping
	public ApiResponse list(@AuthenticationPrincipal AuthenticatedActor actor, @Valid @ModelAttribute NoticeListRequest query, HttpServletRequest request) {
		var response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}
	@GetMapping("/{id}")
	public ApiResponse get(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id, HttpServletRequest request) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}
	@PostMapping(consumes = "multipart/form-data")
	public ApiResponse create(@AuthenticationPrincipal AuthenticatedActor actor, @Valid @RequestPart("data") NoticeSaveRequest body,
		@RequestPart(value = "attachments", required = false) List<MultipartFile> files, HttpServletRequest request) {
		return ApiResponse.success(service.save(actor, null, body.toCommand(), attachmentSources(files), AuthRequestSupport.clientContext(request)), RequestTrace.traceId(request));
	}
	@PatchMapping(value = "/{id}", consumes = "multipart/form-data")
	public ApiResponse update(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id,
		@Valid @RequestPart("data") NoticeSaveRequest body, @RequestPart(value = "attachments", required = false) List<MultipartFile> files, HttpServletRequest request) {
		return ApiResponse.success(service.save(actor, id, body.toCommand(), attachmentSources(files), AuthRequestSupport.clientContext(request)), RequestTrace.traceId(request));
	}
	@DeleteMapping("/{id}")
	public ApiResponse delete(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id, HttpServletRequest request) {
		service.delete(actor, id, AuthRequestSupport.clientContext(request)); return ApiResponse.success(null, RequestTrace.traceId(request));
	}
	@GetMapping("/{id}/histories")
	public ApiResponse histories(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id,
		@Valid @ModelAttribute NoticeListRequest query, HttpServletRequest request) {
		var response = service.histories(actor, id, query.pageValue(), query.perPageValue());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}
	@PostMapping(value = "/editor-images", consumes = "multipart/form-data")
	public ApiResponse upload(@AuthenticationPrincipal AuthenticatedActor actor, @RequestPart("image") MultipartFile file, HttpServletRequest request) {
		return ApiResponse.success(media.upload(actor, MultipartMediaFileSource.from(file)), RequestTrace.traceId(request));
	}
	@DeleteMapping("/editor-images")
	public ApiResponse cleanup(@AuthenticationPrincipal AuthenticatedActor actor, @Valid @RequestBody NoticeImageCleanupRequest body, HttpServletRequest request) {
		media.removeTemporary(actor, body.mediaIds()); return ApiResponse.success(null, RequestTrace.traceId(request));
	}
	@GetMapping("/media/{mediaId}/content")
	public ResponseEntity<InputStreamResource> content(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long mediaId,
		@RequestParam(defaultValue = "false") boolean download) {
		return NoticeFileResponse.from(media.content(actor, null, mediaId), download);
	}

	private List<MediaFileSource> attachmentSources(List<MultipartFile> files) {
		return files == null ? List.of() : files.stream().<MediaFileSource>map(MultipartMediaFileSource::new).toList();
	}
}
