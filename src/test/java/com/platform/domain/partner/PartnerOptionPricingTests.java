package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistOption;
import com.platform.domain.specialist.SpecialistScheduleMode;
import com.platform.domain.specialist.SpecialistStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PartnerOptionPricingTests {

	@Test
	void draftPartnerCanOwnAnOptionWithoutASpecialist() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);

		assertThat(partner.allowStatus()).isEqualTo(PartnerAllowStatus.DRAFT);
		assertThat(option.partner()).isSameAs(partner);
		assertThat(option.regularPrice()).isEqualByComparingTo("30000");
		assertThat(option.salePrice()).isEqualByComparingTo("24000");
		assertThat(option.effectivePrice()).isEqualByComparingTo("24000");
		assertThat(option.discountRate()).isEqualTo(20);
	}

	@Test
	void specialistInheritsThePartnerOptionPricingWithoutAnOverride() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);
		SpecialistOption assignment = new SpecialistOption(
			specialist(partner),
			option,
			null,
			null
		);

		assertThat(assignment.effectiveRegularPrice()).isEqualByComparingTo("30000");
		assertThat(assignment.effectiveSalePrice()).isEqualByComparingTo("24000");
		assertThat(assignment.effectivePrice()).isEqualByComparingTo("24000");
		assertThat(assignment.effectiveDiscountRate()).isEqualTo(20);
	}

	@Test
	void specialistCanUseACustomRegularAndSalePrice() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);
		SpecialistOption assignment = new SpecialistOption(
			specialist(partner),
			option,
			new BigDecimal("45000"),
			new BigDecimal("36000")
		);

		assertThat(assignment.effectiveRegularPrice()).isEqualByComparingTo("45000");
		assertThat(assignment.effectiveSalePrice()).isEqualByComparingTo("36000");
		assertThat(assignment.effectivePrice()).isEqualByComparingTo("36000");
		assertThat(assignment.effectiveDiscountRate()).isEqualTo(20);
	}

	@Test
	void specialistCustomRegularPriceDoesNotInheritThePartnerSalePrice() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);
		SpecialistOption assignment = new SpecialistOption(
			specialist(partner),
			option,
			new BigDecimal("45000"),
			null
		);

		assertThat(assignment.effectiveRegularPrice()).isEqualByComparingTo("45000");
		assertThat(assignment.effectiveSalePrice()).isNull();
		assertThat(assignment.effectivePrice()).isEqualByComparingTo("45000");
		assertThat(assignment.effectiveDiscountRate()).isNull();
	}

	private PartnerOption option(Partner partner) {
		return new PartnerOption(
			partner,
			"Gel nail",
			null,
			new BigDecimal("30000"),
			new BigDecimal("24000"),
			60,
			true,
			0
		);
	}

	private Specialist specialist(Partner partner) {
		return new Specialist(
			partner,
			0,
			"Nail artist",
			null,
			null,
			null,
			SpecialistField.NAIL_ARTIST,
			null,
			SpecialistScheduleMode.INHERIT_PARTNER_HOURS,
			null,
			"{\"enabled\":false}",
			SpecialistStatus.VISIBLE,
			SpecialistAllowStatus.APPROVED
		);
	}
}
