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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "partner_business_registrations")
public class PartnerBusinessRegistration extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_id", nullable = false, unique = true)
	private Partner partner;

	@Column(name = "business_number", length = 20, unique = true)
	private String businessNumber;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "ceo_name", length = 100)
	private String ceoName;

	@Column(name = "opening_date")
	private LocalDate openingDate;

	@Column(name = "settlement_bank_name", length = 50)
	private String settlementBankName;

	@Column(name = "settlement_account_number", length = 50)
	private String settlementAccountNumber;

	@Column(name = "settlement_account_holder", length = 100)
	private String settlementAccountHolder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerBusinessRegistrationStatus status = PartnerBusinessRegistrationStatus.ACTIVE;

	protected PartnerBusinessRegistration() {
	}

	public PartnerBusinessRegistration(
		String businessNumber,
		String companyName,
		String ceoName,
		LocalDate openingDate,
		String settlementBankName,
		String settlementAccountNumber,
		String settlementAccountHolder
	) {
		this.businessNumber = businessNumber;
		this.companyName = companyName;
		this.ceoName = ceoName;
		this.openingDate = openingDate;
		this.settlementBankName = settlementBankName;
		this.settlementAccountNumber = settlementAccountNumber;
		this.settlementAccountHolder = settlementAccountHolder;
	}

	void assignPartner(Partner partner) {
		this.partner = partner;
	}

	public void update(
		String businessNumber,
		String companyName,
		String ceoName,
		LocalDate openingDate,
		String settlementBankName,
		String settlementAccountNumber,
		String settlementAccountHolder
	) {
		this.businessNumber = businessNumber;
		this.companyName = companyName;
		this.ceoName = ceoName;
		this.openingDate = openingDate;
		this.settlementBankName = settlementBankName;
		this.settlementAccountNumber = settlementAccountNumber;
		this.settlementAccountHolder = settlementAccountHolder;
	}

	public Long id() {
		return id;
	}

	public String businessNumber() {
		return businessNumber;
	}

	public String companyName() {
		return companyName;
	}

	public String ceoName() {
		return ceoName;
	}

	public LocalDate openingDate() {
		return openingDate;
	}

	public String settlementBankName() {
		return settlementBankName;
	}

	public String settlementAccountNumber() {
		return settlementAccountNumber;
	}

	public String settlementAccountHolder() {
		return settlementAccountHolder;
	}

	public PartnerBusinessRegistrationStatus status() {
		return status;
	}
}
