package com.platform.infrastructure.persistence.media;

import com.platform.domain.media.Media;
import com.platform.domain.media.MediaOwnerType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MediaRepository extends JpaRepository<Media, Long> {

	Optional<Media> findByIdAndDeletedAtIsNull(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Media> findLockedByIdAndDeletedAtIsNull(Long id);

	List<Media> findByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Media> findLockedByOwnerTypeAndOwnerIdAndCollectionAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection
	);

	List<Media> findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(MediaOwnerType ownerType, Long ownerId);

	List<Media> findByOwnerTypeAndOwnerIdInAndCollectionAndDeletedAtIsNullOrderByOwnerIdAscSortOrderAscIdAsc(
		MediaOwnerType ownerType,
		Set<Long> ownerIds,
		String collection
	);
}
