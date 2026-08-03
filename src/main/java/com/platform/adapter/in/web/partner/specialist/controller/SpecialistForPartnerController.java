package com.platform.adapter.in.web.partner.specialist.controller;

import com.platform.adapter.in.web.partner.specialist.request.SpecialistListForPartnerRequest;
import com.platform.adapter.in.web.partner.specialist.request.SpecialistCreateForPartnerRequest;
import com.platform.adapter.in.web.partner.specialist.request.SpecialistStatusUpdateForPartnerRequest;
import com.platform.adapter.in.web.partner.specialist.request.SpecialistUpdateForPartnerRequest;
import com.platform.application.specialist.SpecialistForPartnerService;
import com.platform.application.specialist.SpecialistMediaForPartnerService;
import com.platform.application.specialist.result.SpecialistDeletedResult;
import com.platform.application.media.result.MediaContentResult;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.PaginatedResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/partner/specialists")
public class SpecialistForPartnerController {

	private final SpecialistForPartnerService service;
	private final SpecialistMediaForPartnerService mediaService;

	public SpecialistForPartnerController(
		SpecialistForPartnerService service,
		SpecialistMediaForPartnerService mediaService
	) {
		this.service = service;
		this.mediaService = mediaService;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute SpecialistListForPartnerRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute SpecialistCreateForPartnerRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@RequestMapping(
		value = "/{id}",
		method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH},
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute SpecialistUpdateForPartnerRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(actor, id, body.toCommand(request.getParameterMap().keySet())),
			RequestTrace.traceId(request)
		);
	}

	@PatchMapping("/{id}/status")
	public ApiResponse changeStatus(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody SpecialistStatusUpdateForPartnerRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.changeStatus(actor, id, body.status()), RequestTrace.traceId(request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		SpecialistDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}

	@GetMapping("/{specialistId}/media/{mediaId}/content")
	public ResponseEntity<InputStreamResource> mediaContent(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long specialistId,
		@PathVariable Long mediaId
	) {
		MediaContentResult result = mediaService.content(actor, specialistId, mediaId);
		ContentDisposition disposition = ContentDisposition.inline()
			.filename(result.originalName(), StandardCharsets.UTF_8)
			.build();

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(result.mimeType()))
			.contentLength(result.size())
			.cacheControl(CacheControl.noStore())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			.header("X-Content-Type-Options", "nosniff")
			.body(new InputStreamResource(result.content().inputStream()));
	}
}
