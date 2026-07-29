package com.medi.application.media.storage;

public interface MediaStorage {

	StoredMediaFile store(MediaFileSource source);

	MediaContent load(String path);

	void delete(String path);
}
