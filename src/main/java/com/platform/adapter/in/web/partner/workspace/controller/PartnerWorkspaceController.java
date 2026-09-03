package com.platform.adapter.in.web.partner.workspace.controller;

import com.platform.adapter.in.web.partner.workspace.request.CreateOwnedPartnerRequest;
import com.platform.application.category.CategoryForPartnerService;
import com.platform.application.partner.BusinessRegistrationOcrService;
import com.platform.application.partner.PartnerWorkspaceService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import com.platform.common.web.multipart.MultipartMediaFileSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/partner/partners")
public class PartnerWorkspaceController {

	private final PartnerWorkspaceService service;
	private final CategoryForPartnerService categoryService;
	private final BusinessRegistrationOcrService ocrService;

	public PartnerWorkspaceController(
		PartnerWorkspaceService service,
		CategoryForPartnerService categoryService,
		BusinessRegistrationOcrService ocrService
	) {
		this.service = service;
		this.categoryService = categoryService;
		this.ocrService = ocrService;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.list(actor), RequestTrace.traceId(request));
	}

	@GetMapping("/registration-categories")
	public ApiResponse registrationCategories(
		@AuthenticationPrincipal AuthenticatedActor actor,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			categoryService.registrationCategories(actor),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping(value = "/business-registration/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse analyzeBusinessRegistration(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@RequestPart("business_registration_file") MultipartFile file,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			ocrService.analyze(actor, MultipartMediaFileSource.from(file)),
			RequestTrace.traceId(request)
		);
	}

	@GetMapping("/business-registration/availability")
	public ApiResponse businessNumberAvailability(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@RequestParam("business_number") String businessNumber,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			ocrService.availability(actor, businessNumber),
			RequestTrace.traceId(request)
		);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute CreateOwnedPartnerRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}
}
