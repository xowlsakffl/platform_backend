package com.medi.adapter.in.web.staff.media.controller;

import com.medi.adapter.in.web.staff.media.request.MediaListRequest;
import com.medi.adapter.in.web.staff.media.request.MediaUpdateRequest;
import com.medi.adapter.in.web.staff.media.request.MediaUploadRequest;
import com.medi.application.media.MediaStaffService;
import com.medi.application.media.result.MediaContentResult;
import com.medi.application.media.result.MediaDeletedResult;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/staff/media")
public class MediaStaffController {

	private final MediaStaffService service;

	public MediaStaffController(MediaStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute MediaListRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.list(actor, query.toQuery()), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}/content")
	public ResponseEntity<InputStreamResource> content(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id
	) {
		MediaContentResult result = service.content(actor, id);
		ContentDisposition disposition = ContentDisposition.inline()
			.filename(result.originalName(), StandardCharsets.UTF_8)
			.build();

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(result.mimeType()))
			.contentLength(result.size())
			.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			.header("X-Content-Type-Options", "nosniff")
			.body(new InputStreamResource(result.content().inputStream()));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse upload(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute MediaUploadRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.upload(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping("/{id}")
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody MediaUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.update(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		MediaDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
