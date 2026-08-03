package com.platform.application.media.storage;

public interface MediaStorage {

	StoredMediaFile store(MediaFileSource source);

	MediaContent load(String path);

	void delete(String path);
}
