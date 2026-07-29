package com.medi.adapter.in.web.staff.category.controller;

import com.medi.adapter.in.web.staff.category.request.CategoryCreateRequest;
import com.medi.adapter.in.web.staff.category.request.CategoryListRequest;
import com.medi.adapter.in.web.staff.category.request.CategoryMultipartCreateRequest;
import com.medi.adapter.in.web.staff.category.request.CategoryMultipartUpdateRequest;
import com.medi.adapter.in.web.staff.category.request.CategorySelectorRequest;
import com.medi.adapter.in.web.staff.category.request.CategoryUpdateRequest;
import com.medi.application.category.CategoryStaffService;
import com.medi.application.category.result.CategoryDeletedResult;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.ApiResponse;
import com.medi.common.web.PaginatedResponse;
import com.medi.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/staff/categories")
public class CategoryStaffController {

	private final CategoryStaffService service;

	public CategoryStaffController(CategoryStaffService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse list(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute CategoryListRequest query,
		HttpServletRequest request
	) {
		PaginatedResponse<?> response = service.list(actor, query.toQuery());
		return ApiResponse.success(response.items(), response.meta(), RequestTrace.traceId(request));
	}

	@GetMapping("/selector")
	public ApiResponse selector(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute CategorySelectorRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.selector(actor, query.toQuery()), RequestTrace.traceId(request));
	}

	@GetMapping("/{id}")
	public ApiResponse get(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.get(actor, id), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse create(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @RequestBody CategoryCreateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse createMultipart(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@Valid @ModelAttribute CategoryMultipartCreateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.create(actor, body.toCommand()), RequestTrace.traceId(request));
	}

	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse update(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @RequestBody CategoryUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(service.update(actor, id, body.toCommand()), RequestTrace.traceId(request));
	}

	@PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse updateMultipart(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		@Valid @ModelAttribute CategoryMultipartUpdateRequest body,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.update(
				actor,
				id,
				body.toCommand(
					request.getParameterMap().containsKey("code"),
					request.getParameterMap().containsKey("group_code")
				)
			),
			RequestTrace.traceId(request)
		);
	}

	@DeleteMapping("/{id}")
	public ApiResponse delete(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long id,
		HttpServletRequest request
	) {
		CategoryDeletedResult response = service.delete(actor, id);
		return ApiResponse.success(response, RequestTrace.traceId(request));
	}
}
