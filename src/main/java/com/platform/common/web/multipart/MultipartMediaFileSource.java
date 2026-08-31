package com.platform.common.web.multipart;

import com.platform.application.media.storage.MediaFileSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record MultipartMediaFileSource(MultipartFile file) implements MediaFileSource {

	public static MediaFileSource from(MultipartFile file) {
		return file == null || file.isEmpty() ? null : new MultipartMediaFileSource(file);
	}

	public static List<MediaFileSource> from(List<MultipartFile> files) {
		return files == null
			? List.of()
			: files.stream().map(MultipartMediaFileSource::from).filter(java.util.Objects::nonNull).toList();
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
