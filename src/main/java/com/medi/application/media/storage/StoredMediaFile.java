package com.medi.application.media.storage;

import com.medi.domain.media.MediaDisk;

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
