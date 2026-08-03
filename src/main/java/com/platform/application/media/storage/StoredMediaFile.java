package com.platform.application.media.storage;

import com.platform.domain.media.MediaDisk;

public record StoredMediaFile(
	MediaDisk disk,
	String path,
	String originalName,
	String mimeType,
	long size,
	Integer width,
	Integer height
) {
}
