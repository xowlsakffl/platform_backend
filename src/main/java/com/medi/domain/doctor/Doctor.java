package com.medi.domain.doctor;

import com.medi.domain.common.BaseTimeEntity;
import com.medi.domain.hospital.Hospital;
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
@Table(name = "hospital_doctors")
public class Doctor extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hospital_id", nullable = false)
	private Hospital hospital;

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

	@Column(name = "license_number", nullable = false, unique = true, length = 100)
	private String licenseNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "specialist_field", nullable = false, length = 80)
	private DoctorSpecialistField specialistField;

	@Column(columnDefinition = "json")
	private String educations;

	@Column(columnDefinition = "json")
	private String careers;

	@Column(name = "etc_contents", columnDefinition = "json")
	private String etcContents;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DoctorStatus status = DoctorStatus.HIDDEN;

	@Enumerated(EnumType.STRING)
	@Column(name = "allow_status", nullable = false, length = 20)
	private DoctorAllowStatus allowStatus = DoctorAllowStatus.PENDING;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected Doctor() {
	}

	public Doctor(
		Hospital hospital,
		int sortOrder,
		String name,
		String gender,
		String position,
		LocalDate careerStartedAt,
		String licenseNumber,
		DoctorSpecialistField specialistField,
		String educations,
		String careers,
		String etcContents,
		DoctorStatus status,
		DoctorAllowStatus allowStatus
	) {
		this.hospital = hospital;
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
		this.status = status == null ? DoctorStatus.HIDDEN : status;
		this.allowStatus = allowStatus == null ? DoctorAllowStatus.PENDING : allowStatus;
	}

	public void update(
		Hospital hospital,
		int sortOrder,
		String name,
		String gender,
		String position,
		LocalDate careerStartedAt,
		String licenseNumber,
		DoctorSpecialistField specialistField,
		String educations,
		String careers,
		String etcContents,
		DoctorStatus status,
		DoctorAllowStatus allowStatus
	) {
		this.hospital = hospital;
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

	public void changeStatus(DoctorStatus status) {
		this.status = status;
	}

	public void changeAllowStatus(DoctorAllowStatus allowStatus) {
		this.allowStatus = allowStatus;
	}

	public void softDelete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
			status = DoctorStatus.HIDDEN;
		}
	}

	public Long id() {
		return id;
	}

	public Hospital hospital() {
		return hospital;
	}

	public Long hospitalId() {
		return hospital.id();
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

	public DoctorSpecialistField specialistField() {
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

	public DoctorStatus status() {
		return status;
	}

	public DoctorAllowStatus allowStatus() {
		return allowStatus;
	}

	public long viewCount() {
		return viewCount;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}
}
