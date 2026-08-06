package com.platform.domain.hashtag;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "hashtag_relations")
public class HashtagRelation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hashtag_id", nullable = false)
	private Hashtag hashtag;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 30)
	private HashtagTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "sort_order", nullable = false)
	private byte sortOrder;

	protected HashtagRelation() {
	}

	public HashtagRelation(Hashtag hashtag, HashtagTargetType targetType, Long targetId, int sortOrder) {
		this.hashtag = Objects.requireNonNull(hashtag);
		this.targetType = Objects.requireNonNull(targetType);
		this.targetId = Objects.requireNonNull(targetId);
		this.sortOrder = (byte) sortOrder;
	}

	public Long id() {
		return id;
	}

	public Hashtag hashtag() {
		return hashtag;
	}

	public HashtagTargetType targetType() {
		return targetType;
	}

	public Long targetId() {
		return targetId;
	}

	public int sortOrder() {
		return sortOrder;
	}
}
