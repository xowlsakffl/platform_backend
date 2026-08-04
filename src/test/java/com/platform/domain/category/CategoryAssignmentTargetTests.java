package com.platform.domain.category;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryAssignmentTargetTests {

	@Test
	void partnerUsesAnActiveRootCategoryFromThePartnerUsage() {
		Category category = category(null, (byte) 1, "미용실", "KB_HAIR_SALON");

		assertThat(CategoryAssignmentTarget.PARTNER.usage())
			.isEqualTo(CategoryUsageType.PARTNER_CATEGORY);
		assertThat(CategoryAssignmentTarget.PARTNER.accepts(category)).isTrue();
		assertThat(CategoryAssignmentTarget.PARTNER_OPTION.accepts(category)).isFalse();
	}

	@Test
	void partnerOptionUsesAnActiveChildCategoryFromTheOptionUsage() {
		Category parent = category(null, (byte) 1, "미용실", "KB_HAIR_SALON");
		Category category = category(parent, (byte) 2, "커트", "KB_HAIR_CUT");

		assertThat(CategoryAssignmentTarget.PARTNER_OPTION.usage())
			.isEqualTo(CategoryUsageType.PARTNER_OPTION_CATEGORY);
		assertThat(CategoryAssignmentTarget.PARTNER_OPTION.accepts(category)).isTrue();
		assertThat(CategoryAssignmentTarget.PARTNER.accepts(category)).isFalse();
	}

	@Test
	void inactiveCategoryCannotBeAssigned() {
		Category category = new Category(
			CategoryDomain.PARTNER,
			null,
			(byte) 1,
			CategoryGroup.TREATMENT,
			"미용실",
			"KB_HAIR_SALON",
			"미용실",
			1,
			CategoryStatus.INACTIVE,
			true
		);

		assertThat(CategoryAssignmentTarget.PARTNER.accepts(category)).isFalse();
	}

	private Category category(Category parent, byte depth, String name, String code) {
		return new Category(
			CategoryDomain.PARTNER,
			parent,
			depth,
			CategoryGroup.TREATMENT,
			name,
			code,
			parent == null ? name : parent.name() + " > " + name,
			1,
			CategoryStatus.ACTIVE,
			true
		);
	}
}
