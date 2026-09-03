package com.platform.adapter.in.web.partner.category.controller;

import com.platform.adapter.in.web.partner.category.request.CategorySelectorForPartnerRequest;
import com.platform.application.category.CategoryForPartnerService;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.ApiResponse;
import com.platform.common.web.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/partner/partners/{partnerId}/categories")
public class CategoryForPartnerController {

	private final CategoryForPartnerService service;

	public CategoryForPartnerController(CategoryForPartnerService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse selector(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@Valid @ModelAttribute CategorySelectorForPartnerRequest query,
		HttpServletRequest request
	) {
		return ApiResponse.success(
			service.selector(actor, partnerId, query.usage(), query.parentId(), query.q()),
			RequestTrace.traceId(request)
		);
	}
}
