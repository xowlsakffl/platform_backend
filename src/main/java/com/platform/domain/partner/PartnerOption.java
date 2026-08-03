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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_options")
public class PartnerOption extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "price_type", nullable = false, length = 20)
	private PartnerPriceType priceType;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	@Column(name = "is_visible", nullable = false)
	private boolean visible;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected PartnerOption() {
	}

	public PartnerOption(
		Partner partner,
		String name,
		String description,
		BigDecimal price,
		PartnerPriceType priceType,
		Integer durationMinutes,
		boolean visible,
		int sortOrder
	) {
		this.partner = partner;
		update(name, description, price, priceType, durationMinutes, visible, sortOrder);
	}

	public void update(
		String name,
		String description,
		BigDecimal price,
		PartnerPriceType priceType,
		Integer durationMinutes,
		boolean visible,
		int sortOrder
	) {
		this.name = name;
		this.description = description;
		this.priceType = priceType;
		this.price = priceType == PartnerPriceType.INQUIRE ? null : price;
		this.durationMinutes = durationMinutes;
		this.visible = visible;
		this.sortOrder = sortOrder;
	}

	public void softDelete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
			visible = false;
		}
	}

	public Long id() {
		return id;
	}

	public Partner partner() {
		return partner;
	}

	public Long partnerId() {
		return partner.id();
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public BigDecimal price() {
		return price;
	}

	public PartnerPriceType priceType() {
		return priceType;
	}

	public Integer durationMinutes() {
		return durationMinutes;
	}

	public boolean visible() {
		return visible;
	}

	public int sortOrder() {
		return sortOrder;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}
}
