package com.medi.adapter.in.web.hospital.doctor.controller;

import com.medi.adapter.in.web.hospital.doctor.request.DoctorListForHospitalRequest;
import com.medi.adapter.in.web.hospital.doctor.request.DoctorCreateForHospitalRequest;
import com.medi.adapter.in.web.hospital.doctor.request.DoctorStatusUpdateForHospitalRequest;
import com.medi.adapter.in.web.hospital.doctor.request.DoctorUpdateForHospitalRequest;
import com.medi.application.doctor.DoctorForHospitalService;
import com.medi.application.doctor.DoctorMediaForHospitalService;
import com.medi.application.doctor.result.DoctorDeletedResult;
import com.medi.application.media.result.MediaContentResult;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.PaginatedResponse;
import com.medi.common.web.RequestTrace;
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
@RequestMapping("/api/v1/hospital/doctors")
public class DoctorForHospitalController {

	private final DoctorForHospitalService service;
	private final DoctorMediaForHospitalService mediaService;

	public DoctorForHospitalController(
		DoctorForHospitalService service,
		DoctorMediaForHospitalService mediaService
	) {
		this.service = service;
		this.mediaService = mediaService;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute DoctorListForHospitalRequest query,
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
		@Valid @ModelAttribute DoctorCreateForHospitalRequest body,
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
		@Valid @ModelAttribute DoctorUpdateForHospitalRequest body,
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
		@Valid @RequestBody DoctorStatusUpdateForHospitalRequest body,
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
		DoctorDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}

	@GetMapping("/{doctorId}/media/{mediaId}/content")
	public ResponseEntity<InputStreamResource> mediaContent(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long doctorId,
		@PathVariable Long mediaId
	) {
		MediaContentResult result = mediaService.content(actor, doctorId, mediaId);
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
