package com.platform.domain.partner;

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

@Entity
@Table(name = "partner_links")
public class PartnerLink extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Enumerated(EnumType.STRING)
	@Column(name = "link_type", nullable = false, length = 40)
	private PartnerLinkType type;

	@Column(nullable = false, length = 1000)
	private String url;

	@Column(name = "sort_order", nullable = false)
	private byte sortOrder;

	protected PartnerLink() {
	}

	public PartnerLink(Partner partner, PartnerLinkType type, String url, int sortOrder) {
		this.partner = partner;
		this.type = type;
		this.url = url;
		this.sortOrder = (byte) sortOrder;
	}

	public Long id() {
		return id;
	}

	public Long partnerId() {
		return partner.id();
	}

	public PartnerLinkType type() {
		return type;
	}

	public String url() {
		return url;
	}

	public int sortOrder() {
		return sortOrder;
	}
}
