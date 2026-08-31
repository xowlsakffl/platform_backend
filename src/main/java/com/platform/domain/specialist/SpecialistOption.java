package com.platform.domain.specialist;

import com.platform.domain.common.BaseTimeEntity;
import com.platform.domain.partner.PartnerOption;
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

@Entity
@Table(name = "specialist_options")
public class SpecialistOption extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partner_option_id", nullable = false)
	private PartnerOption partnerOption;

	@Column(name = "regular_price_override", precision = 12, scale = 2)
	private BigDecimal regularPriceOverride;

	@Column(name = "sale_price_override", precision = 12, scale = 2)
	private BigDecimal salePriceOverride;

	protected SpecialistOption() {
	}

	public SpecialistOption(
		Specialist specialist,
		PartnerOption partnerOption,
		BigDecimal regularPriceOverride,
		BigDecimal salePriceOverride
	) {
		this.specialist = specialist;
		this.partnerOption = partnerOption;
		this.regularPriceOverride = regularPriceOverride;
		this.salePriceOverride = regularPriceOverride == null ? null : salePriceOverride;
	}

	public Long id() {
		return id;
	}

	public Specialist specialist() {
		return specialist;
	}

	public PartnerOption partnerOption() {
		return partnerOption;
	}

	public BigDecimal regularPriceOverride() {
		return regularPriceOverride;
	}

	public BigDecimal salePriceOverride() {
		return salePriceOverride;
	}

	public BigDecimal effectiveRegularPrice() {
		return regularPriceOverride == null ? partnerOption.regularPrice() : regularPriceOverride;
	}

	public BigDecimal effectiveSalePrice() {
		return regularPriceOverride == null ? partnerOption.salePrice() : salePriceOverride;
	}

	public BigDecimal effectivePrice() {
		BigDecimal salePrice = effectiveSalePrice();
		return salePrice == null ? effectiveRegularPrice() : salePrice;
	}

	public Integer effectiveDiscountRate() {
		BigDecimal regularPrice = effectiveRegularPrice();
		BigDecimal salePrice = effectiveSalePrice();
		if (salePrice == null || regularPrice.signum() <= 0 || salePrice.compareTo(regularPrice) >= 0) {
			return null;
		}
		return regularPrice.subtract(salePrice)
			.multiply(BigDecimal.valueOf(100))
			.divide(regularPrice, 0, RoundingMode.DOWN)
			.intValue();
	}

}
