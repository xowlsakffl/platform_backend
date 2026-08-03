package com.platform.domain.specialist;

import com.platform.domain.common.BaseTimeEntity;
import com.platform.domain.partner.PartnerOption;
import com.platform.domain.partner.PartnerPriceType;
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

	@Column(name = "price_override", precision = 12, scale = 2)
	private BigDecimal priceOverride;

	@Enumerated(EnumType.STRING)
	@Column(name = "price_type_override", length = 20)
	private PartnerPriceType priceTypeOverride;

	protected SpecialistOption() {
	}

	public SpecialistOption(
		Specialist specialist,
		PartnerOption partnerOption,
		BigDecimal priceOverride,
		PartnerPriceType priceTypeOverride
	) {
		this.specialist = specialist;
		this.partnerOption = partnerOption;
		this.priceTypeOverride = priceTypeOverride;
		this.priceOverride = priceTypeOverride == PartnerPriceType.INQUIRE ? null : priceOverride;
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

	public BigDecimal priceOverride() {
		return priceOverride;
	}

	public PartnerPriceType priceTypeOverride() {
		return priceTypeOverride;
	}

	public BigDecimal effectivePrice() {
		if (effectivePriceType() == PartnerPriceType.INQUIRE) {
			return null;
		}
		return priceOverride == null ? partnerOption.price() : priceOverride;
	}

	public PartnerPriceType effectivePriceType() {
		return priceTypeOverride == null ? partnerOption.priceType() : priceTypeOverride;
	}
}
