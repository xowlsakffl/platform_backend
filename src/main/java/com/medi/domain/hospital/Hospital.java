package com.medi.domain.hospital;

import com.medi.domain.account.AccountHospital;
import com.medi.domain.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "hospitals")
public class Hospital extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "youtube_link", length = 500)
	private String youtubeLink;

	private String address;

	@Column(name = "address_detail")
	private String addressDetail;

	private String latitude;

	private String longitude;

	@Column(name = "consulting_hours", columnDefinition = "text")
	private String consultingHours;

	@Column(name = "operation_hours", columnDefinition = "json")
	private String operationHours;

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
	private HospitalAllowStatus allowStatus = HospitalAllowStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private HospitalStatus status = HospitalStatus.ACTIVE;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HospitalContact> contacts = new LinkedHashSet<>();

	@OneToOne(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private HospitalBusinessRegistration businessRegistration;

	@OneToOne(mappedBy = "hospital", fetch = FetchType.LAZY)
	private AccountHospital accountHospital;

	@ManyToMany
	@JoinTable(
		name = "hospital_feature_assignments",
		joinColumns = @JoinColumn(name = "hospital_id"),
		inverseJoinColumns = @JoinColumn(name = "hospital_feature_id")
	)
	private Set<HospitalFeature> features = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "hospital_interpretation_languages",
		joinColumns = @JoinColumn(name = "hospital_id")
	)
	@Enumerated(EnumType.STRING)
	@Column(name = "language", nullable = false, length = 30)
	private Set<HospitalInterpretationLanguage> interpretationLanguages = new LinkedHashSet<>();

	protected Hospital() {
	}

	public Hospital(
		String name,
		String description,
		String youtubeLink,
		String address,
		String addressDetail,
		String latitude,
		String longitude,
		String consultingHours,
		String operationHours,
		String direction,
		HospitalAllowStatus allowStatus,
		HospitalStatus status
	) {
		this.name = name;
		this.description = description;
		this.youtubeLink = youtubeLink;
		this.address = address;
		this.addressDetail = addressDetail;
		this.latitude = latitude;
		this.longitude = longitude;
		this.consultingHours = consultingHours;
		this.operationHours = operationHours;
		this.direction = direction;
		this.allowStatus = allowStatus == null ? HospitalAllowStatus.PENDING : allowStatus;
		this.status = status == null ? HospitalStatus.ACTIVE : status;
	}

	public void updateProfile(
		String description,
		String youtubeLink,
		String address,
		String addressDetail,
		String latitude,
		String longitude,
		String consultingHours,
		String operationHours,
		String direction,
		HospitalAllowStatus allowStatus,
		HospitalStatus status
	) {
		this.description = description;
		this.youtubeLink = youtubeLink;
		this.address = address;
		this.addressDetail = addressDetail;
		this.latitude = latitude;
		this.longitude = longitude;
		this.consultingHours = consultingHours;
		this.operationHours = operationHours;
		this.direction = direction;
		if (allowStatus != null) {
			this.allowStatus = allowStatus;
		}
		if (status != null) {
			this.status = status;
		}
	}

	public void changeStatus(HospitalStatus status) {
		this.status = status;
	}

	public void changeAllowStatus(HospitalAllowStatus allowStatus) {
		this.allowStatus = allowStatus;
	}

	public void replaceContacts(Set<HospitalContact> contacts) {
		this.contacts.clear();
		for (HospitalContact contact : contacts) {
			contact.assignHospital(this);
			this.contacts.add(contact);
		}
	}

	public void replaceBusinessRegistration(HospitalBusinessRegistration businessRegistration) {
		if (businessRegistration != null) {
			businessRegistration.assignHospital(this);
		}
		this.businessRegistration = businessRegistration;
	}

	public void replaceFeatures(Set<HospitalFeature> features) {
		this.features.clear();
		this.features.addAll(features);
	}

	public void replaceInterpretationLanguages(Set<HospitalInterpretationLanguage> interpretationLanguages) {
		this.interpretationLanguages.clear();
		this.interpretationLanguages.addAll(interpretationLanguages);
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

	public String description() {
		return description;
	}

	public String youtubeLink() {
		return youtubeLink;
	}

	public String address() {
		return address;
	}

	public String addressDetail() {
		return addressDetail;
	}

	public String latitude() {
		return latitude;
	}

	public String longitude() {
		return longitude;
	}

	public String consultingHours() {
		return consultingHours;
	}

	public String operationHours() {
		return operationHours;
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

	public HospitalAllowStatus allowStatus() {
		return allowStatus;
	}

	public HospitalStatus status() {
		return status;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}

	public Set<HospitalContact> contacts() {
		return contacts;
	}

	public HospitalBusinessRegistration businessRegistration() {
		return businessRegistration;
	}

	public AccountHospital accountHospital() {
		return accountHospital;
	}

	public Set<HospitalFeature> features() {
		return features;
	}

	public Set<HospitalInterpretationLanguage> interpretationLanguages() {
		return interpretationLanguages;
	}
}
