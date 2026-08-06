package com.platform.application.hashtag;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.hashtag.Hashtag;
import com.platform.domain.hashtag.HashtagRelation;
import com.platform.domain.hashtag.HashtagTargetType;
import com.platform.infrastructure.persistence.hashtag.HashtagRelationRepository;
import com.platform.infrastructure.persistence.hashtag.HashtagRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HashtagAssignmentService {

	private final HashtagRepository hashtagRepository;
	private final HashtagRelationRepository relationRepository;

	public HashtagAssignmentService(
		HashtagRepository hashtagRepository,
		HashtagRelationRepository relationRepository
	) {
		this.hashtagRepository = hashtagRepository;
		this.relationRepository = relationRepository;
	}

	@Transactional
	public void replace(HashtagTargetType targetType, Long targetId, List<String> rawValues) {
		LinkedHashMap<String, String> values = normalize(rawValues);
		relationRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
		relationRepository.flush();

		int sortOrder = 0;
		for (var entry : values.entrySet()) {
			Hashtag hashtag = hashtagRepository.findByNormalizedName(entry.getKey())
				.orElseGet(() -> hashtagRepository.save(new Hashtag(entry.getValue(), entry.getKey())));
			relationRepository.save(new HashtagRelation(hashtag, targetType, targetId, sortOrder++));
		}
	}

	@Transactional(readOnly = true)
	public List<String> values(HashtagTargetType targetType, Long targetId) {
		return relationRepository.findByTargetTypeAndTargetIdOrderBySortOrderAscIdAsc(targetType, targetId)
			.stream()
			.map(relation -> relation.hashtag().name())
			.toList();
	}

	private LinkedHashMap<String, String> normalize(List<String> rawValues) {
		LinkedHashMap<String, String> values = new LinkedHashMap<>();
		for (String rawValue : rawValues == null ? List.<String>of() : rawValues) {
			String value = rawValue == null ? null : rawValue.replaceFirst("^#+", "").trim();
			if (value == null || value.isEmpty()) {
				continue;
			}
			if (value.length() > 30) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "해시태그는 30자 이하로 입력해 주세요.");
			}
			values.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
		}
		if (values.size() > 10) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "해시태그는 최대 10개까지 등록할 수 있습니다.");
		}
		return values;
	}
}
