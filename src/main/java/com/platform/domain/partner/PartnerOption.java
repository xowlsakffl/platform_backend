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
import java.math.BigDecimal;
import java.math.RoundingMode;
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

	@Column(nullable = false, length = 80)
	private String name;

	@Column(length = 200)
	private String description;

	@Column(name = "regular_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal regularPrice;

	@Column(name = "sale_price", precision = 12, scale = 2)
	private BigDecimal salePrice;

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
		BigDecimal regularPrice,
		BigDecimal salePrice,
		Integer durationMinutes,
		boolean visible,
		int sortOrder
	) {
		this.partner = partner;
		update(name, description, regularPrice, salePrice, durationMinutes, visible, sortOrder);
	}

	public void update(
		String name,
		String description,
		BigDecimal regularPrice,
		BigDecimal salePrice,
		Integer durationMinutes,
		boolean visible,
		int sortOrder
	) {
		this.name = name;
		this.description = description;
		this.regularPrice = regularPrice;
		this.salePrice = salePrice;
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

	public BigDecimal regularPrice() {
		return regularPrice;
	}

	public BigDecimal salePrice() {
		return salePrice;
	}

	public BigDecimal effectivePrice() {
		return salePrice == null ? regularPrice : salePrice;
	}

	public Integer discountRate() {
		if (salePrice == null || regularPrice.signum() <= 0 || salePrice.compareTo(regularPrice) >= 0) {
			return null;
		}
		return regularPrice.subtract(salePrice)
			.multiply(BigDecimal.valueOf(100))
			.divide(regularPrice, 0, RoundingMode.DOWN)
			.intValue();
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
