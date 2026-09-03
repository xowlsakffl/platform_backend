package com.platform.infrastructure.persistence.notice;

import com.platform.domain.notice.Notice;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {
	Optional<Notice> findByIdAndDeletedAtIsNull(Long id);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Notice> findLockedByIdAndDeletedAtIsNull(Long id);
}
