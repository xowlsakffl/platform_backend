package com.platform.adapter.in.web.partner.notice.controller;

import com.platform.application.notice.NoticeMediaService;
import com.platform.application.notice.NoticeService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import com.platform.common.web.notice.NoticeFileResponse;
import com.platform.common.web.notice.NoticeListRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partner/notices")
public class NoticeForPartnerController {
	private final NoticeService service;
	private final NoticeMediaService media;
	public NoticeForPartnerController(NoticeService service, NoticeMediaService media) { this.service = service; this.media = media; }
	@GetMapping
	public ResponseEntity<ApiResponse> list(@AuthenticationPrincipal AuthenticatedActor actor, @Valid @ModelAttribute NoticeListRequest query, HttpServletRequest request) {
		var response = service.list(actor, query.toQuery());
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request)));
	}
	@GetMapping("/popups")
	public ResponseEntity<ApiResponse> popups(@AuthenticationPrincipal AuthenticatedActor actor, HttpServletRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.popups(actor), RequestTrace.traceId(request)));
	}
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse> get(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id, HttpServletRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request)));
	}
	@GetMapping("/{id}/media/{mediaId}/content")
	public ResponseEntity<InputStreamResource> content(@AuthenticationPrincipal AuthenticatedActor actor, @PathVariable Long id,
		@PathVariable Long mediaId, @RequestParam(defaultValue = "false") boolean download) {
		return NoticeFileResponse.from(media.content(actor, id, mediaId), download);
	}
}
