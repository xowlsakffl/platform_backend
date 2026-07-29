package com.medi.application.media.result;

import com.medi.application.media.storage.MediaContent;

public record MediaContentResult(
	String originalName,
	String mimeType,
	long size,
	MediaContent content
) {
}
