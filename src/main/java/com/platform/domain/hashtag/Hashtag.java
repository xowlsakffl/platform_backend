package com.platform.domain.hashtag;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "hashtags")
public class Hashtag extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(name = "normalized_name", nullable = false, unique = true, length = 30)
	private String normalizedName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private HashtagStatus status = HashtagStatus.ACTIVE;

	protected Hashtag() {
	}

	public Hashtag(String name, String normalizedName) {
		this.name = Objects.requireNonNull(name);
		this.normalizedName = Objects.requireNonNull(normalizedName);
	}

	public Long id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String normalizedName() {
		return normalizedName;
	}

	public HashtagStatus status() {
		return status;
	}
}
