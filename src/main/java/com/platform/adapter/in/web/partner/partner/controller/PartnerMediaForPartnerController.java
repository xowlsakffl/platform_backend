package com.platform.adapter.in.web.partner.partner.controller;

import com.platform.application.media.result.MediaContentResult;
import com.platform.application.partner.PartnerMediaForPartnerService;
import com.platform.common.security.AuthenticatedActor;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partner/partners/{partnerId}/media")
public class PartnerMediaForPartnerController {

	private final PartnerMediaForPartnerService service;

	public PartnerMediaForPartnerController(PartnerMediaForPartnerService service) {
		this.service = service;
	}

	@GetMapping("/{mediaId}/content")
	public ResponseEntity<InputStreamResource> content(
		@AuthenticationPrincipal AuthenticatedActor actor,
		@PathVariable Long partnerId,
		@PathVariable Long mediaId
	) {
		MediaContentResult result = service.content(actor, partnerId, mediaId);
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
