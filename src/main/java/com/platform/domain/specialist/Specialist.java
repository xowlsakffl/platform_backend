package com.platform.domain.specialist;

import com.platform.domain.account.AccountStaff;
import com.platform.domain.common.BaseTimeEntity;
import com.platform.domain.partner.Partner;
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
import java.util.Objects;

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

	@Enumerated(EnumType.STRING)
	@Column(name = "specialist_field", nullable = false, length = 80)
	private SpecialistField specialistField;

	@Column(length = 500)
	private String introduction;

	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_mode", nullable = false, length = 30)
	private SpecialistScheduleMode scheduleMode = SpecialistScheduleMode.INHERIT_PARTNER_HOURS;

	@Column(name = "operation_hours", columnDefinition = "json")
	private String operationHours;

	@Column(name = "holiday_policy", nullable = false, columnDefinition = "json")
	private String holidayPolicy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SpecialistStatus status = SpecialistStatus.HIDDEN;

	@Enumerated(EnumType.STRING)
	@Column(name = "allow_status", nullable = false, length = 20)
	private SpecialistAllowStatus allowStatus = SpecialistAllowStatus.REVIEW_REQUESTED;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewer_staff_id")
	private AccountStaff reviewerStaff;

	@Column(name = "review_started_at")
	private LocalDateTime reviewStartedAt;

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
		SpecialistField specialistField,
		String introduction,
		SpecialistScheduleMode scheduleMode,
		String operationHours,
		String holidayPolicy,
		SpecialistStatus status,
		SpecialistAllowStatus allowStatus
	) {
		this.partner = partner;
		this.sortOrder = sortOrder;
		this.name = name;
		this.gender = gender;
		this.position = position;
		this.careerStartedAt = careerStartedAt;
		this.specialistField = specialistField;
		this.introduction = introduction;
		this.scheduleMode = scheduleMode;
		this.operationHours = operationHours;
		this.holidayPolicy = holidayPolicy;
		this.status = status == null ? SpecialistStatus.HIDDEN : status;
		this.allowStatus = allowStatus == null ? SpecialistAllowStatus.REVIEW_REQUESTED : allowStatus;
	}

	public void update(
		String name,
		String gender,
		String position,
		LocalDate careerStartedAt,
		SpecialistField specialistField,
		String introduction,
		SpecialistScheduleMode scheduleMode,
		String operationHours,
		String holidayPolicy,
		SpecialistStatus status,
		SpecialistAllowStatus allowStatus
	) {
		this.name = name;
		this.gender = gender;
		this.position = position;
		this.careerStartedAt = careerStartedAt;
		this.specialistField = specialistField;
		this.introduction = introduction;
		this.scheduleMode = scheduleMode;
		this.operationHours = operationHours;
		this.holidayPolicy = holidayPolicy;
		this.status = status;
		this.allowStatus = allowStatus;
	}

	public void changeStatus(SpecialistStatus status) {
		this.status = status;
	}

	public void changeSortOrder(int sortOrder) {
		if (sortOrder < 0) {
			throw new IllegalArgumentException("Specialist sort order must not be negative.");
		}
		this.sortOrder = sortOrder;
	}

	public void requestReview() {
		this.allowStatus = SpecialistAllowStatus.REVIEW_REQUESTED;
		this.reviewerStaff = null;
		this.reviewStartedAt = null;
	}

	public void startReview(AccountStaff reviewerStaff) {
		this.allowStatus = SpecialistAllowStatus.IN_REVIEW;
		this.reviewerStaff = Objects.requireNonNull(reviewerStaff);
		this.reviewStartedAt = LocalDateTime.now();
	}

	public void completeReview(SpecialistAllowStatus decision) {
		if (decision != SpecialistAllowStatus.APPROVED && decision != SpecialistAllowStatus.REJECTED) {
			throw new IllegalArgumentException("Review decision must be APPROVED or REJECTED.");
		}
		this.allowStatus = decision;
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

	public SpecialistField specialistField() {
		return specialistField;
	}

	public String introduction() {
		return introduction;
	}

	public SpecialistScheduleMode scheduleMode() {
		return scheduleMode;
	}

	public String operationHours() {
		return operationHours;
	}

	public String holidayPolicy() {
		return holidayPolicy;
	}

	public SpecialistStatus status() {
		return status;
	}

	public SpecialistAllowStatus allowStatus() {
		return allowStatus;
	}

	public AccountStaff reviewerStaff() {
		return reviewerStaff;
	}

	public LocalDateTime reviewStartedAt() {
		return reviewStartedAt;
	}

	public long viewCount() {
		return viewCount;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}
}
