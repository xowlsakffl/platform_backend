package com.platform.domain.partner;

import com.platform.domain.account.AccountPartner;
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
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(
	name = "partner_memberships",
	uniqueConstraints = @UniqueConstraint(
		name = "partner_memberships_account_partner_unique",
		columnNames = {"account_partner_id", "partner_id"}
	)
)
public class PartnerMembership extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_partner_id", nullable = false)
	private AccountPartner accountPartner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerMembershipRole role = PartnerMembershipRole.OWNER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerMembershipStatus status = PartnerMembershipStatus.ACTIVE;

	protected PartnerMembership() {
	}

	public static PartnerMembership owner(AccountPartner accountPartner, Partner partner) {
		PartnerMembership membership = new PartnerMembership();
		membership.accountPartner = Objects.requireNonNull(accountPartner);
		membership.partner = Objects.requireNonNull(partner);
		return membership;
	}

	public void activateAsOwner() {
		this.role = PartnerMembershipRole.OWNER;
		this.status = PartnerMembershipStatus.ACTIVE;
	}

	public void deactivate() {
		this.status = PartnerMembershipStatus.INACTIVE;
	}

	public Long id() {
		return id;
	}

	public AccountPartner accountPartner() {
		return accountPartner;
	}

	public Long accountPartnerId() {
		return accountPartner == null ? null : accountPartner.id();
	}

	public Partner partner() {
		return partner;
	}

	public Long partnerId() {
		return partner == null ? null : partner.id();
	}

	public PartnerMembershipRole role() {
		return role;
	}

	public PartnerMembershipStatus status() {
		return status;
	}
}
