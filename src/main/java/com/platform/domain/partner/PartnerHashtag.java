package com.platform.domain.partner;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_hashtags")
public class PartnerHashtag extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Column(nullable = false, length = 30)
	private String value;

	@Column(name = "sort_order", nullable = false)
	private byte sortOrder;

	protected PartnerHashtag() {
	}

	public PartnerHashtag(Partner partner, String value, int sortOrder) {
		this.partner = partner;
		this.value = value;
		this.sortOrder = (byte) sortOrder;
	}

	public Long id() {
		return id;
	}

	public Long partnerId() {
		return partner.id();
	}

	public String value() {
		return value;
	}

	public int sortOrder() {
		return sortOrder;
	}
}
