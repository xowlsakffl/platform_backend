package com.platform.domain.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistField;
import com.platform.domain.specialist.SpecialistOption;
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
		assertThat(option.price()).isEqualByComparingTo("30000");
	}

	@Test
	void specialistCanOverrideOnlyThePriceAndInheritThePriceType() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);
		SpecialistOption assignment = new SpecialistOption(
			specialist(partner),
			option,
			new BigDecimal("45000"),
			null
		);

		assertThat(assignment.effectivePrice()).isEqualByComparingTo("45000");
		assertThat(assignment.effectivePriceType()).isEqualTo(PartnerPriceType.FIXED);
	}

	@Test
	void specialistCanOverrideTheOptionAsPriceInquiry() {
		Partner partner = Partner.createDraft("Draft partner");
		PartnerOption option = option(partner);
		SpecialistOption assignment = new SpecialistOption(
			specialist(partner),
			option,
			null,
			PartnerPriceType.INQUIRE
		);

		assertThat(assignment.effectivePrice()).isNull();
		assertThat(assignment.effectivePriceType()).isEqualTo(PartnerPriceType.INQUIRE);
	}

	private PartnerOption option(Partner partner) {
		return new PartnerOption(
			partner,
			"Gel nail",
			null,
			new BigDecimal("30000"),
			PartnerPriceType.FIXED,
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
			null,
			SpecialistField.NAIL_ARTIST,
			null,
			null,
			null,
			SpecialistStatus.VISIBLE,
			SpecialistAllowStatus.APPROVED
		);
	}
}
