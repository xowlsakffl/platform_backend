package com.platform.adapter.in.web.user.media.controller;

import com.platform.application.media.MediaContentForUserService;
import com.platform.application.media.result.MediaContentResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/media")
public class MediaContentForUserController {

	private final MediaContentForUserService service;

	public MediaContentForUserController(MediaContentForUserService service) {
		this.service = service;
	}

	@GetMapping("/{id}/content")
	public ResponseEntity<InputStreamResource> content(@PathVariable Long id) {
		MediaContentResult result = service.content(id);
		ContentDisposition disposition = ContentDisposition.inline()
			.filename(result.originalName(), StandardCharsets.UTF_8)
			.build();

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(result.mimeType()))
			.contentLength(result.size())
			.cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
			.header("X-Content-Type-Options", "nosniff")
			.body(new InputStreamResource(result.content().inputStream()));
	}
}
