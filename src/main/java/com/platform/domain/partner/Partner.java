package com.platform.domain.partner;

import com.platform.domain.account.AccountStaff;
import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "partners")
public class Partner extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(name = "english_name", length = 90)
	private String englishName;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "road_address")
	private String roadAddress;

	@Column(name = "detail_address")
	private String detailAddress;

	@Column(name = "subway_stations", columnDefinition = "json")
	private String subwayStations;

	@Column(name = "region_sort_key")
	private String regionSortKey;

	private String latitude;

	private String longitude;

	@Column(name = "operating_hours_notice", columnDefinition = "text")
	private String operatingHoursNotice;

	@Column(name = "operation_hours", columnDefinition = "json")
	private String operationHours;

	@Column(name = "holiday_policy", columnDefinition = "json")
	private String holidayPolicy;

	@Column(columnDefinition = "text")
	private String direction;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "evaluation_count", nullable = false)
	private int evaluationCount;

	@Column(name = "evaluation_average_rating", nullable = false, precision = 2, scale = 1)
	private BigDecimal evaluationAverageRating = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(name = "allow_status", nullable = false, length = 20)
	private PartnerAllowStatus allowStatus = PartnerAllowStatus.REVIEW_REQUESTED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PartnerStatus status = PartnerStatus.ACTIVE;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_staff_id")
	private AccountStaff assignedStaff;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewer_staff_id")
	private AccountStaff reviewerStaff;

	@Column(name = "review_started_at")
	private LocalDateTime reviewStartedAt;

	@OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<PartnerContact> contacts = new LinkedHashSet<>();

	@OneToOne(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private PartnerBusinessRegistration businessRegistration;

	@ManyToMany
	@JoinTable(
		name = "partner_feature_assignments",
		joinColumns = @JoinColumn(name = "partner_id"),
		inverseJoinColumns = @JoinColumn(name = "partner_feature_id")
	)
	private Set<PartnerFeature> features = new LinkedHashSet<>();

	protected Partner() {
	}

	public static Partner createDraft(String name) {
		Partner partner = new Partner(
			name,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			PartnerAllowStatus.DRAFT,
			PartnerStatus.ACTIVE
		);
		return partner;
	}

	public Partner(
		String name,
		String description,
		String roadAddress,
		String latitude,
		String longitude,
		String operatingHoursNotice,
		String operationHours,
		String direction,
		PartnerAllowStatus allowStatus,
		PartnerStatus status
	) {
		this.name = name;
		this.description = description;
		this.roadAddress = roadAddress;
		refreshRegionSortKey();
		this.latitude = latitude;
		this.longitude = longitude;
		this.operatingHoursNotice = operatingHoursNotice;
		this.operationHours = operationHours;
		this.direction = direction;
		this.allowStatus = allowStatus == null ? PartnerAllowStatus.REVIEW_REQUESTED : allowStatus;
		this.status = status == null ? PartnerStatus.ACTIVE : status;
	}

	public void updateProfile(
		String description,
		String roadAddress,
		String latitude,
		String longitude,
		String operatingHoursNotice,
		String operationHours,
		String direction,
		PartnerAllowStatus allowStatus,
		PartnerStatus status
	) {
		this.description = description;
		this.roadAddress = roadAddress;
		refreshRegionSortKey();
		this.latitude = latitude;
		this.longitude = longitude;
		this.operatingHoursNotice = operatingHoursNotice;
		this.operationHours = operationHours;
		this.direction = direction;
		if (allowStatus != null) {
			this.allowStatus = allowStatus;
		}
		if (status != null) {
			this.status = status;
		}
	}

	public void changeStatus(PartnerStatus status) {
		this.status = status;
	}

	public void updateOnboardingProfile(
		String name,
		String englishName,
		String description,
		String roadAddress,
		String detailAddress,
		String latitude,
		String longitude,
		String operatingHoursNotice,
		String operationHours,
		String direction
	) {
		this.name = name;
		this.englishName = englishName;
		this.description = description;
		this.roadAddress = roadAddress;
		this.detailAddress = detailAddress;
		refreshRegionSortKey();
		this.latitude = latitude;
		this.longitude = longitude;
		this.operatingHoursNotice = operatingHoursNotice;
		this.operationHours = operationHours;
		this.direction = direction;
	}

	public void requestReview() {
		this.allowStatus = PartnerAllowStatus.REVIEW_REQUESTED;
		this.reviewerStaff = null;
		this.reviewStartedAt = null;
	}

	public boolean restartReviewAfterCriticalInformationChange() {
		if (allowStatus != PartnerAllowStatus.IN_REVIEW
			&& allowStatus != PartnerAllowStatus.APPROVED) {
			return false;
		}
		requestReview();
		return true;
	}

	public void startReview(AccountStaff reviewerStaff) {
		this.allowStatus = PartnerAllowStatus.IN_REVIEW;
		this.reviewerStaff = Objects.requireNonNull(reviewerStaff);
		this.reviewStartedAt = LocalDateTime.now();
	}

	public void completeReview(PartnerAllowStatus decision) {
		if (decision != PartnerAllowStatus.APPROVED && decision != PartnerAllowStatus.REJECTED) {
			throw new IllegalArgumentException("Review decision must be APPROVED or REJECTED.");
		}
		this.allowStatus = decision;
	}

	public void changeDetailAddress(String detailAddress) {
		this.detailAddress = detailAddress;
	}

	public void changeNames(String name, String englishName) {
		this.name = name;
		this.englishName = englishName;
	}

	public void changeHolidayPolicy(String holidayPolicy) {
		this.holidayPolicy = holidayPolicy;
	}

	public void changeOperationHours(String operationHours) {
		this.operationHours = operationHours;
	}

	public void changeSubwayStations(String subwayStations) {
		this.subwayStations = subwayStations;
	}

	private void refreshRegionSortKey() {
		String address = trimToNull(roadAddress);
		if (address == null) {
			this.regionSortKey = null;
			return;
		}
		String[] parts = address.split("\\s+");
		if (parts.length < 2) {
			this.regionSortKey = address;
			return;
		}
		int length = parts.length >= 3 && parts[1].endsWith("시") && parts[2].endsWith("구") ? 3 : 2;
		this.regionSortKey = String.join(" ", java.util.Arrays.copyOf(parts, length));
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String trimToNull(String value) {
		return hasText(value) ? value.trim() : null;
	}

	public void replaceContacts(Set<PartnerContact> contacts) {
		this.contacts.clear();
		for (PartnerContact contact : contacts) {
			contact.assignPartner(this);
			this.contacts.add(contact);
		}
	}

	public void replaceBusinessRegistration(PartnerBusinessRegistration businessRegistration) {
		if (businessRegistration != null) {
			businessRegistration.assignPartner(this);
		}
		this.businessRegistration = businessRegistration;
	}

	public void replaceFeatures(Set<PartnerFeature> features) {
		this.features.clear();
		this.features.addAll(features);
	}

	public void assignStaff(AccountStaff assignedStaff) {
		this.assignedStaff = assignedStaff;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public Long id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String englishName() {
		return englishName;
	}

	public String description() {
		return description;
	}

	public String roadAddress() {
		return roadAddress;
	}

	public String detailAddress() {
		return detailAddress;
	}

	public String subwayStations() {
		return subwayStations;
	}

	public String regionSortKey() {
		return regionSortKey;
	}

	public String latitude() {
		return latitude;
	}

	public String longitude() {
		return longitude;
	}

	public String operatingHoursNotice() {
		return operatingHoursNotice;
	}

	public String operationHours() {
		return operationHours;
	}

	public String holidayPolicy() {
		return holidayPolicy;
	}

	public String direction() {
		return direction;
	}

	public long viewCount() {
		return viewCount;
	}

	public int evaluationCount() {
		return evaluationCount;
	}

	public BigDecimal evaluationAverageRating() {
		return evaluationAverageRating;
	}

	public PartnerAllowStatus allowStatus() {
		return allowStatus;
	}

	public PartnerStatus status() {
		return status;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}

	public AccountStaff assignedStaff() {
		return assignedStaff;
	}

	public AccountStaff reviewerStaff() {
		return reviewerStaff;
	}

	public LocalDateTime reviewStartedAt() {
		return reviewStartedAt;
	}

	public Set<PartnerContact> contacts() {
		return contacts;
	}

	public PartnerBusinessRegistration businessRegistration() {
		return businessRegistration;
	}

	public Set<PartnerFeature> features() {
		return features;
	}

}
