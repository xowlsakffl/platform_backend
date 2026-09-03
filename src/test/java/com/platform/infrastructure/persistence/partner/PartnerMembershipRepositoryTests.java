package com.platform.infrastructure.persistence.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerMembership;
import com.platform.domain.partner.PartnerMembershipStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
class PartnerMembershipRepositoryTests {

	@Autowired
	private AccountPartnerRepository accountRepository;

	@Autowired
	private PartnerRepository partnerRepository;

	@Autowired
	private PartnerMembershipRepository membershipRepository;

	@Test
	void oneAccountCanOwnAndListMultiplePartners() {
		AccountPartner account = accountRepository.saveAndFlush(AccountPartner.create(
			"파트너 계정",
			"multi_partner",
			"multi-partner@platform.local",
			null,
			"encoded-password",
			AccountPartnerStatus.ACTIVE
		));
		Partner first = partnerRepository.saveAndFlush(Partner.createDraft("첫 번째 업체"));
		Partner second = partnerRepository.saveAndFlush(Partner.createDraft("두 번째 업체"));
		membershipRepository.saveAndFlush(PartnerMembership.owner(account, first));
		membershipRepository.saveAndFlush(PartnerMembership.owner(account, second));

		List<PartnerMembership> memberships = membershipRepository.findAllForAccount(
			account.id(),
			PartnerMembershipStatus.ACTIVE
		);

		assertEquals(List.of(first.id(), second.id()), memberships.stream()
			.map(PartnerMembership::partnerId)
			.toList());
		assertTrue(membershipRepository.existsByAccountPartner_IdAndPartner_IdAndStatus(
			account.id(),
			second.id(),
			PartnerMembershipStatus.ACTIVE
		));
	}

	@Test
	void ownershipCanMoveToAnExistingInactiveMembership() {
		AccountPartner currentAccount = accountRepository.saveAndFlush(AccountPartner.create(
			"현재 소유자",
			"current_owner",
			"current-owner@platform.local",
			null,
			"encoded-password",
			AccountPartnerStatus.ACTIVE
		));
		AccountPartner nextAccount = accountRepository.saveAndFlush(AccountPartner.create(
			"새 소유자",
			"next_owner",
			"next-owner@platform.local",
			null,
			"encoded-password",
			AccountPartnerStatus.ACTIVE
		));
		Partner partner = partnerRepository.saveAndFlush(Partner.createDraft("소유권 변경 업체"));
		PartnerMembership current = membershipRepository.saveAndFlush(
			PartnerMembership.owner(currentAccount, partner)
		);
		PartnerMembership next = PartnerMembership.owner(nextAccount, partner);
		next.deactivate();
		next = membershipRepository.saveAndFlush(next);

		current.deactivate();
		next.activateAsOwner();
		membershipRepository.saveAndFlush(current);
		membershipRepository.saveAndFlush(next);

		List<PartnerMembership> activeMemberships = membershipRepository.findAllForPartnerForUpdate(
			partner.id(),
			PartnerMembershipStatus.ACTIVE
		);
		assertEquals(1, activeMemberships.size());
		assertEquals(nextAccount.id(), activeMemberships.getFirst().accountPartnerId());
	}
}
