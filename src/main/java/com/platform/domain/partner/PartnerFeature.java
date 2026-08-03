package com.platform.domain.partner;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_features")
public class PartnerFeature extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerFeatureStatus status = PartnerFeatureStatus.ACTIVE;

	protected PartnerFeature() {
	}

	public Long id() {
		return id;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public int sortOrder() {
		return sortOrder;
	}

	public PartnerFeatureStatus status() {
		return status;
	}
}
