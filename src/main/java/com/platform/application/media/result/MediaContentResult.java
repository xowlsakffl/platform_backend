package com.platform.application.media.result;

import com.platform.application.media.storage.MediaContent;

public record MediaContentResult(
	String originalName,
	String mimeType,
	long size,
	MediaContent content
) {
}
