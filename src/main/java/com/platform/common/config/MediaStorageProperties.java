package com.platform.common.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.media.storage")
public class MediaStorageProperties {

	private String root = "./storage/media";
	private DataSize maxFileSize = DataSize.ofMegabytes(20);
	private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
		"image/jpeg",
		"image/png",
		"image/webp",
		"image/gif",
		"application/pdf"
	));

	public String root() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public DataSize maxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(DataSize maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public Set<String> allowedContentTypes() {
		return Set.copyOf(allowedContentTypes);
	}

	public void setAllowedContentTypes(Set<String> allowedContentTypes) {
		this.allowedContentTypes = new LinkedHashSet<>(allowedContentTypes);
	}
}
