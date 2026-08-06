package com.platform.infrastructure.persistence.hashtag;

import com.platform.domain.hashtag.Hashtag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

	Optional<Hashtag> findByNormalizedName(String normalizedName);
}
