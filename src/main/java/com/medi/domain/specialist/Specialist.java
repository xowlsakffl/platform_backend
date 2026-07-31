package com.medi.domain.specialist;

import com.medi.domain.common.BaseTimeEntity;
import com.medi.domain.partner.Partner;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_specialists")
public class Specialist extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "partner_id", nullable = false)
	private Partner partner;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 20)
	private String gender;

	@Column(length = 50)
	private String position;

	@Column(name = "career_started_at")
	private LocalDate careerStartedAt;

	@Column(name = "license_number", unique = true, length = 100)
	private String licenseNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "specialist_field", nullable = false, length = 80)
	private SpecialistField specialistField;

	@Column(columnDefinition = "json")
	private String educations;

	@Column(columnDefinition = "json")
	private String careers;

	@Column(name = "etc_contents", columnDefinition = "json")
	private String etcContents;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SpecialistStatus status = SpecialistStatus.HIDDEN;

	@Enumerated(EnumType.STRING)
	@Column(name = "allow_status", nullable = false, length = 20)
	private SpecialistAllowStatus allowStatus = SpecialistAllowStatus.PENDING;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected Specialist() {
	}

	public Specialist(
		Partner partner,
		int sortOrder,
		String name,
		String gender,
		String position,
		LocalDate careerStartedAt,
		String licenseNumber,
		SpecialistField specialistField,
		String educations,
		String careers,
		String etcContents,
		SpecialistStatus status,
		SpecialistAllowStatus allowStatus
	) {
		this.partner = partner;
		this.sortOrder = sortOrder;
		this.name = name;
		this.gender = gender;
		this.position = position;
		this.careerStartedAt = careerStartedAt;
		this.licenseNumber = licenseNumber;
		this.specialistField = specialistField;
		this.educations = educations;
		this.careers = careers;
		this.etcContents = etcContents;
		this.status = status == null ? SpecialistStatus.HIDDEN : status;
		this.allowStatus = allowStatus == null ? SpecialistAllowStatus.PENDING : allowStatus;
	}

	public void update(
		Partner partner,
		int sortOrder,
		String name,
		String gender,
		String position,
		LocalDate careerStartedAt,
		String licenseNumber,
		SpecialistField specialistField,
		String educations,
		String careers,
		String etcContents,
		SpecialistStatus status,
		SpecialistAllowStatus allowStatus
	) {
		this.partner = partner;
		this.sortOrder = sortOrder;
		this.name = name;
		this.gender = gender;
		this.position = position;
		this.careerStartedAt = careerStartedAt;
		this.licenseNumber = licenseNumber;
		this.specialistField = specialistField;
		this.educations = educations;
		this.careers = careers;
		this.etcContents = etcContents;
		this.status = status;
		this.allowStatus = allowStatus;
	}

	public void changeStatus(SpecialistStatus status) {
		this.status = status;
	}

	public void changeAllowStatus(SpecialistAllowStatus allowStatus) {
		this.allowStatus = allowStatus;
	}

	public void softDelete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
			status = SpecialistStatus.HIDDEN;
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

	public int sortOrder() {
		return sortOrder;
	}

	public String name() {
		return name;
	}

	public String gender() {
		return gender;
	}

	public String position() {
		return position;
	}

	public LocalDate careerStartedAt() {
		return careerStartedAt;
	}

	public String licenseNumber() {
		return licenseNumber;
	}

	public SpecialistField specialistField() {
		return specialistField;
	}

	public String educations() {
		return educations;
	}

	public String careers() {
		return careers;
	}

	public String etcContents() {
		return etcContents;
	}

	public SpecialistStatus status() {
		return status;
	}

	public SpecialistAllowStatus allowStatus() {
		return allowStatus;
	}

	public long viewCount() {
		return viewCount;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}
}
