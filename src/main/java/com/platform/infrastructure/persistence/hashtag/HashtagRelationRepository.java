package com.platform.infrastructure.persistence.hashtag;

import com.platform.domain.hashtag.HashtagRelation;
import com.platform.domain.hashtag.HashtagTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRelationRepository extends JpaRepository<HashtagRelation, Long> {

	@EntityGraph(attributePaths = "hashtag")
	List<HashtagRelation> findByTargetTypeAndTargetIdOrderBySortOrderAscIdAsc(
		HashtagTargetType targetType,
		Long targetId
	);

	void deleteByTargetTypeAndTargetId(HashtagTargetType targetType, Long targetId);
}
