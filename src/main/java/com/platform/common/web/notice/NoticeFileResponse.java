package com.platform.common.web.notice;

import com.platform.application.media.result.MediaContentResult;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class NoticeFileResponse {
	private NoticeFileResponse() {}
	public static ResponseEntity<InputStreamResource> from(MediaContentResult result, boolean download) {
		var disposition = download || !result.mimeType().startsWith("image/") ? ContentDisposition.attachment() : ContentDisposition.inline();
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.mimeType())).contentLength(result.size())
			.cacheControl(CacheControl.noStore())
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(result.originalName(), StandardCharsets.UTF_8).build().toString())
			.header("X-Content-Type-Options", "nosniff").header("Content-Security-Policy", "default-src 'none'; sandbox")
			.body(new InputStreamResource(result.content().inputStream()));
	}
}
