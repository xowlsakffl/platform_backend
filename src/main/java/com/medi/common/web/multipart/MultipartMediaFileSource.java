package com.medi.common.web.multipart;

import com.medi.application.media.storage.MediaFileSource;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public record MultipartMediaFileSource(MultipartFile file) implements MediaFileSource {

	public static MediaFileSource from(MultipartFile file) {
		return file == null || file.isEmpty() ? null : new MultipartMediaFileSource(file);
	}

	@Override
	public String originalFilename() {
		return file.getOriginalFilename();
	}

	@Override
	public String contentType() {
		return file.getContentType();
	}

	@Override
	public long size() {
		return file.getSize();
	}

	@Override
	public InputStream openStream() throws IOException {
		return file.getInputStream();
	}
}
