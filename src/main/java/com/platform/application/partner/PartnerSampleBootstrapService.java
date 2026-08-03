package com.platform.application.partner;

import com.platform.common.error.InternalApplicationException;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.account.AccountStaffStatus;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerBusinessRegistration;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerContactType;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerIndustry;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile("local")
public class PartnerSampleBootstrapService {

	private static final String OPERATION_HOURS = """
		{"monday":"09:30-18:30","tuesday":"09:30-18:30","wednesday":"09:30-18:30","thursday":"09:30-18:30","friday":"09:30-20:00","saturday":"09:30-14:00","sunday":"휴무"}
		""".trim();

	private static final List<PartnerSample> SAMPLES = List.of(
		new PartnerSample(
			"[샘플] 강남 라움헤어", "서울 강남구 테헤란로 101", "8층", "37.4981", "127.0276",
			PartnerAllowStatus.APPROVED, PartnerStatus.ACTIVE, AccountPartnerStatus.ACTIVE,
			"김강남", "partner01@platform.local", "medi_partner_01", "1209900001",
			PartnerIndustry.HAIR_SALON,
			List.of("PARKING", "PRIVATE_ROOM", "AFTERCARE", "RESERVATION_ONLY"),
			true
		),
		new PartnerSample(
			"[샘플] 신사 누아네일", "서울 강남구 도산대로 202", "4층", "37.5193", "127.0231",
			PartnerAllowStatus.APPROVED, PartnerStatus.ACTIVE, AccountPartnerStatus.ACTIVE,
			"이신사", "partner02@platform.local", "medi_partner_02", "1209900002",
			PartnerIndustry.NAIL_SHOP,
			List.of("NIGHT_OPERATION", "AFTERCARE", "STATION_WITHIN_5_MINUTES"),
			true
		),
		new PartnerSample(
			"[샘플] 청담 벨라왁싱", "서울 강남구 압구정로 303", "2층", "37.5250", "127.0472",
			PartnerAllowStatus.PENDING, PartnerStatus.ACTIVE, AccountPartnerStatus.ACTIVE,
			"박청담", "partner03@platform.local", "medi_partner_03", "1209900003",
			PartnerIndustry.WAXING,
			List.of("PRIVATE_ROOM", "AFTERCARE", "PARKING"),
			false
		),
		new PartnerSample(
			"[샘플] 홍대 잉크스튜디오", "서울 마포구 양화로 404", "6층", "37.5563", "126.9237",
			PartnerAllowStatus.REJECTED, PartnerStatus.ACTIVE, AccountPartnerStatus.BLOCKED,
			"최홍대", "partner04@platform.local", "medi_partner_04", "1209900004",
			PartnerIndustry.TATTOO,
			List.of("AFTERCARE", "WOMEN_SPECIALIST", "STATION_WITHIN_5_MINUTES"),
			false
		),
		new PartnerSample(
			"[샘플] 분당 온에스테틱", "경기 성남시 분당구 판교역로 505", "5층", "37.3947", "127.1112",
			PartnerAllowStatus.APPROVED, PartnerStatus.SUSPENDED, AccountPartnerStatus.ACTIVE,
			"정분당", "partner05@platform.local", "medi_partner_05", "1209900005",
			PartnerIndustry.ESTHETIC,
			List.of("LOCKER", "AFTERCARE", "PARKING"),
			true
		),
		new PartnerSample(
			"[샘플] 잠실 브로우랩", "서울 송파구 올림픽로 606", "3층", "37.5133", "127.1002",
			PartnerAllowStatus.PENDING, PartnerStatus.ACTIVE, AccountPartnerStatus.ACTIVE,
			"한잠실", "partner06@platform.local", "medi_partner_06", "1209900006",
			PartnerIndustry.SEMI_PERMANENT,
			List.of("NIGHT_OPERATION", "WOMEN_SPECIALIST", "PARKING"),
			false
		),
		new PartnerSample(
			"[샘플] 부산 릴렉스마사지", "부산 부산진구 중앙대로 707", "7층", "35.1579", "129.0590",
			PartnerAllowStatus.APPROVED, PartnerStatus.ACTIVE, AccountPartnerStatus.ACTIVE,
			"오부산", "partner07@platform.local", "medi_partner_07", "1209900007",
			PartnerIndustry.MASSAGE,
			List.of("PRIVATE_ROOM", "RESERVATION_ONLY", "AFTERCARE", "WEEKEND_OPERATION"),
			true
		),
		new PartnerSample(
			"[샘플] 대구 포쉬네일", "대구 중구 동성로 808", "9층", "35.8690", "128.5940",
			PartnerAllowStatus.APPROVED, PartnerStatus.WITHDRAWN, AccountPartnerStatus.ACTIVE,
			"임대구", "partner08@platform.local", "medi_partner_08", "1209900008",
			PartnerIndustry.NAIL_SHOP,
			List.of("AFTERCARE", "STATION_WITHIN_5_MINUTES"),
			false
		)
	);

	private final PartnerRepository partnerRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final PartnerFeatureRepository featureRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final AccountStaffRepository accountStaffRepository;
	private final PasswordEncoder passwordEncoder;

	public PartnerSampleBootstrapService(
		PartnerRepository partnerRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		PartnerFeatureRepository featureRepository,
		AccountPartnerRepository accountPartnerRepository,
		AccountStaffRepository accountStaffRepository,
		PasswordEncoder passwordEncoder
	) {
		this.partnerRepository = partnerRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.accountPartnerRepository = accountPartnerRepository;
		this.accountStaffRepository = accountStaffRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public int ensureSamples(String password) {
		if (!StringUtils.hasText(password)) {
			throw new InternalApplicationException("파트너 샘플 계정 비밀번호가 비어 있습니다.");
		}

		Map<String, PartnerFeature> features = loadFeatures();
		AccountStaff defaultAssignedStaff = accountStaffRepository
			.findFirstByStatusAndDeletedAtIsNullOrderByIdAsc(AccountStaffStatus.ACTIVE)
			.orElse(null);
		String encodedPassword = passwordEncoder.encode(password);
		int createdCount = 0;

		for (int index = 0; index < SAMPLES.size(); index++) {
			PartnerSample sample = SAMPLES.get(index);
			Partner existingPartner = partnerRepository.findByName(sample.name()).orElse(null);
			if (existingPartner != null) {
				if (existingPartner.assignedStaff() == null && defaultAssignedStaff != null) {
					existingPartner.assignStaff(defaultAssignedStaff);
				}
				continue;
			}
			if (alreadyExists(sample)) {
				continue;
			}
			createSample(index + 1, sample, encodedPassword, defaultAssignedStaff, features);
			createdCount++;
		}
		return createdCount;
	}

	private void createSample(
		int sequence,
		PartnerSample sample,
		String encodedPassword,
		AccountStaff assignedStaff,
		Map<String, PartnerFeature> features
	) {
		Partner partner = new Partner(
			sample.name(),
			"로컬 개발 환경에서 사용하는 가상 파트너 데이터입니다.",
			sample.address(),
			sample.addressDetail(),
			sample.latitude(),
			sample.longitude(),
			"평일 09:00~20:00, 토요일 09:00~15:00",
			OPERATION_HOURS,
			"지하철역 출구에서 도보 5분 이내",
			sample.allowStatus(),
			sample.partnerStatus()
		);
		partner.changeIndustry(sample.industry());
		partner.replaceContacts(createContacts(sequence, sample.email()));
		partner.replaceBusinessRegistration(createBusinessRegistration(sequence, sample));
		partner.replaceFeatures(resolveCodes(sample.featureCodes(), features));
		partner.assignStaff(assignedStaff);

		Partner savedPartner = partnerRepository.saveAndFlush(partner);
		AccountPartner account = AccountPartner.create(
			savedPartner,
			sample.managerName(),
			sample.nickname(),
			sample.email(),
			"010-9200-%04d".formatted(sequence),
			encodedPassword,
			sample.accountStatus()
		);
		if (sample.loggedIn()) {
			account.markLoggedIn();
		}
		accountPartnerRepository.save(account);
	}

	private boolean alreadyExists(PartnerSample sample) {
		return partnerRepository.existsByName(sample.name())
			|| businessRegistrationRepository.existsByBusinessNumber(sample.businessNumber())
			|| accountPartnerRepository.existsByEmail(sample.email())
			|| accountPartnerRepository.existsByNickname(sample.nickname());
	}

	private Map<String, PartnerFeature> loadFeatures() {
		Set<String> codes = SAMPLES.stream()
			.map(PartnerSample::featureCodes)
			.flatMap(Collection::stream)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<String, PartnerFeature> features = featureRepository
			.findByCodeInAndStatus(codes, PartnerFeatureStatus.ACTIVE)
			.stream()
			.collect(Collectors.toMap(PartnerFeature::code, Function.identity()));
		assertCodesFound("파트너 특징", codes, features.keySet());
		return features;
	}

	private <T> Set<T> resolveCodes(List<String> codes, Map<String, T> values) {
		return codes.stream()
			.map(values::get)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void assertCodesFound(String target, Set<String> expected, Set<String> actual) {
		Set<String> missing = new LinkedHashSet<>(expected);
		missing.removeAll(actual);
		if (!missing.isEmpty()) {
			throw new InternalApplicationException(target + " 기준 데이터가 없습니다: " + String.join(", ", missing));
		}
	}

	private Set<PartnerContact> createContacts(int sequence, String email) {
		String suffix = "%04d".formatted(sequence);
		Set<PartnerContact> contacts = new LinkedHashSet<>();
		contacts.add(new PartnerContact(PartnerContactType.REPRESENTATIVE_PHONE, "02-6100-" + suffix, 0, true));
		contacts.add(new PartnerContact(PartnerContactType.SMS_SENDER_PHONE, "02-6200-" + suffix, 0, true));
		contacts.add(new PartnerContact(PartnerContactType.CALL_RECEIVER_PHONE, "02-6300-" + suffix, 0, true));
		contacts.add(new PartnerContact(PartnerContactType.CONSULTATION_RECEIVER_PHONE, "010-7100-" + suffix, 0, true));
		contacts.add(new PartnerContact(PartnerContactType.CONSULTATION_RECEIVER_PHONE, "010-7200-" + suffix, 1, false));
		contacts.add(new PartnerContact(PartnerContactType.EVENT_NOTICE_RECEIVER_PHONE, "010-7300-" + suffix, 0, true));
		contacts.add(new PartnerContact(PartnerContactType.NOTICE_MARKETING_EMAIL, email, 0, true));
		contacts.add(new PartnerContact(
			PartnerContactType.NOTICE_MARKETING_EMAIL,
			"marketing%02d@platform.local".formatted(sequence),
			1,
			false
		));
		return contacts;
	}

	private PartnerBusinessRegistration createBusinessRegistration(int sequence, PartnerSample sample) {
		return new PartnerBusinessRegistration(
			sample.businessNumber(),
			sample.name(),
			sample.managerName(),
			"서비스업",
			"미용업",
			sample.address(),
			sample.addressDetail(),
			"메디은행",
			"1002-000-%04d".formatted(sequence),
			sample.managerName(),
			"tax%02d@platform.local".formatted(sequence),
			LocalDate.of(2020 + sequence % 5, sequence, Math.min(sequence + 5, 28))
		);
	}

	private record PartnerSample(
		String name,
		String address,
		String addressDetail,
		String latitude,
		String longitude,
		PartnerAllowStatus allowStatus,
		PartnerStatus partnerStatus,
		AccountPartnerStatus accountStatus,
		String managerName,
		String email,
		String nickname,
		String businessNumber,
		PartnerIndustry industry,
		List<String> featureCodes,
		boolean loggedIn
	) {
	}
}
