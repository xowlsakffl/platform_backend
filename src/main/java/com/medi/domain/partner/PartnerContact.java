package com.medi.domain.partner;

import com.medi.domain.common.BaseTimeEntity;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_contacts")
public class PartnerContact extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Enumerated(EnumType.STRING)
	@Column(name = "contact_type", nullable = false, length = 40)
	private PartnerContactType contactType;

	@Column(nullable = false)
	private String value;

	@Column(name = "sort_order", nullable = false)
	private byte sortOrder;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(length = 500)
	private String memo;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected PartnerContact() {
	}

	public PartnerContact(PartnerContactType contactType, String value, int sortOrder, boolean primary) {
		this.contactType = contactType;
		this.value = value;
		this.sortOrder = (byte) sortOrder;
		this.primary = primary;
	}

	void assignPartner(Partner partner) {
		this.partner = partner;
	}

	public Long id() {
		return id;
	}

	public PartnerContactType contactType() {
		return contactType;
	}

	public String value() {
		return value;
	}

	public int sortOrder() {
		return sortOrder;
	}

	public boolean primary() {
		return primary;
	}

	public boolean active() {
		return active;
	}
}
