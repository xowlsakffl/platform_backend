package com.platform.application.media;

import com.platform.application.media.storage.MediaStorage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class MediaFileCleanup {

	private static final Logger log = LoggerFactory.getLogger(MediaFileCleanup.class);

	private final MediaStorage mediaStorage;

	MediaFileCleanup(MediaStorage mediaStorage) {
		this.mediaStorage = mediaStorage;
	}

	void deleteAfterCommit(Collection<String> paths) {
		List<String> targets = paths.stream()
			.filter(Objects::nonNull)
			.filter(path -> !path.isBlank())
			.distinct()
			.toList();
		if (targets.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			delete(targets);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				delete(targets);
			}
		});
	}

	private void delete(List<String> paths) {
		for (String path : paths) {
			try {
				mediaStorage.delete(path);
			} catch (RuntimeException exception) {
				log.warn("삭제된 미디어 원본 파일 정리에 실패했습니다. path={}", path, exception);
			}
		}
	}
}
