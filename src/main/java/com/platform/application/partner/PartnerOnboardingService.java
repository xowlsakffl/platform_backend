package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.hashtag.HashtagAssignmentService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import com.platform.application.partner.command.UpdatePartnerOnboardingCommand;
import com.platform.application.partner.result.PartnerBusinessRegistrationResult;
import com.platform.application.partner.result.PartnerContactResult;
import com.platform.application.partner.result.PartnerFeatureResult;
import com.platform.application.partner.result.PartnerLinkResult;
import com.platform.application.partner.result.PartnerOnboardingResult;
import com.platform.application.partner.result.PartnerSettlementAccountResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerBusinessRegistration;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerContactType;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.domain.hashtag.HashtagTargetType;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import com.platform.infrastructure.persistence.partner.PartnerLinkRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerOnboardingService {

	private final PartnerHistoryService historyService;
	private final PartnerScheduleChangePolicy scheduleChangePolicy;

	private static final String ACTION_SUBMITTED = "ONBOARDING_SUBMITTED";
	private static final String ACTION_REVIEW_RESTARTED = "REVIEW_RESTARTED_AFTER_INFORMATION_CHANGE";
	private static final Set<String> REVIEW_CRITICAL_FIELDS = Set.of(
		"name",
		"category_id",
		"road_address",
		"detail_address",
		"latitude",
		"longitude",
		"representative_phone",
		"representative_email",
		"business_number",
		"company_name",
		"ceo_name",
		"business_registration_file",
		"existing_business_registration_file_id"
	);
	private final OwnershipPolicy ownershipPolicy;
	private final CategoryAssignmentService categoryAssignmentService;
	private final PartnerRepository partnerRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final PartnerFeatureRepository featureRepository;
	private final HashtagAssignmentService hashtagAssignmentService;
	private final PartnerLinkRepository linkRepository;
	private final PartnerLinkAssignmentService linkAssignmentService;
	private final OperationHistoryRepository operationHistoryRepository;
	private final PartnerOptionForPartnerService optionService;
	private final PartnerSchedulePolicyValidator schedulePolicyValidator;
	private final PartnerAccessInformationValidator accessInformationValidator;
	private final PartnerBusinessNumberPolicy businessNumberPolicy;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;
	private final ObjectMapper objectMapper;

	public PartnerOnboardingService(
		PartnerHistoryService historyService,
		PartnerScheduleChangePolicy scheduleChangePolicy,
		OwnershipPolicy ownershipPolicy,
		CategoryAssignmentService categoryAssignmentService,
		PartnerRepository partnerRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		PartnerFeatureRepository featureRepository,
		HashtagAssignmentService hashtagAssignmentService,
		PartnerLinkRepository linkRepository,
		PartnerLinkAssignmentService linkAssignmentService,
		OperationHistoryRepository operationHistoryRepository,
		PartnerOptionForPartnerService optionService,
		PartnerSchedulePolicyValidator schedulePolicyValidator,
		PartnerAccessInformationValidator accessInformationValidator,
		PartnerBusinessNumberPolicy businessNumberPolicy,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		StaffSummaryCacheInvalidator summaryCacheInvalidator,
		ObjectMapper objectMapper
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.categoryAssignmentService = categoryAssignmentService;
		this.partnerRepository = partnerRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.hashtagAssignmentService = hashtagAssignmentService;
		this.linkRepository = linkRepository;
		this.linkAssignmentService = linkAssignmentService;
		this.operationHistoryRepository = operationHistoryRepository;
		this.optionService = optionService;
		this.schedulePolicyValidator = schedulePolicyValidator;
		this.historyService = historyService;
		this.scheduleChangePolicy = scheduleChangePolicy;
		this.accessInformationValidator = accessInformationValidator;
		this.businessNumberPolicy = businessNumberPolicy;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PartnerOnboardingResult get(AuthenticatedActor actor, Long partnerId) {
		Partner partner = ownedPartner(actor, partnerId);
		return result(actor, partner);
	}

	@Transactional
	public PartnerOnboardingResult update(
		AuthenticatedActor actor,
		Long partnerId,
		UpdatePartnerOnboardingCommand command
	) {
		Partner partner = editablePartner(actor, partnerId);
		var before = historyService.capture(partner);
		if (command.specified("operation_hours")) {
			scheduleChangePolicy.assertCompatible(partner.id(),
				schedulePolicyValidator.normalizeOperationHours(command.operationHours(), true));
		}
		String name = command.specified("name")
			? requireText(command.name(), "Partner name cannot be empty.")
			: partner.name();
		String englishName = command.specified("english_name")
			? trimToNull(command.englishName())
			: partner.englishName();
		boolean reviewCriticalInformationChanged = reviewCriticalInformationChanged(partner, command, name);
		partner.updateOnboardingProfile(
			name,
			englishName,
			command.specified("description") ? trimToNull(command.description()) : partner.description(),
			command.specified("road_address") ? trimToNull(command.roadAddress()) : partner.roadAddress(),
			command.specified("detail_address") ? trimToNull(command.detailAddress()) : partner.detailAddress(),
			command.specified("latitude") ? trimToNull(command.latitude()) : partner.latitude(),
			command.specified("longitude") ? trimToNull(command.longitude()) : partner.longitude(),
			command.specified("operating_hours_notice")
				? trimToNull(command.operatingHoursNotice())
				: partner.operatingHoursNotice(),
			command.specified("operation_hours")
				? schedulePolicyValidator.normalizeOperationHours(command.operationHours(), true)
				: partner.operationHours(),
			command.specified("direction") ? trimToNull(command.direction()) : partner.direction()
		);
		if (command.specified("subway_stations")) {
			partner.changeSubwayStations(
				accessInformationValidator.normalizeSubwayStations(command.subwayStations())
			);
		}
		if (command.specified("holiday_policy")) {
			partner.changeHolidayPolicy(
				schedulePolicyValidator.normalizeHolidayPolicy(command.holidayPolicy(), true)
			);
		}

		if (command.contacts() != null) {
			partner.replaceContacts(mergeContacts(partner, command.contacts(), command.specifiedFields()));
		}
		if (command.businessRegistration() != null) {
			updateBusinessRegistration(partner, command.businessRegistration(), command.specifiedFields());
		}
		if (command.featureIds() != null) {
			partner.replaceFeatures(loadFeatures(command.featureIds()));
		}
		partnerRepository.saveAndFlush(partner);
		if (command.specified("category_id")) {
			optionService.validatePartnerCategoryChange(partner.id(), command.categoryId());
			categoryAssignmentService.replacePrimary(
				CategoryAssignmentTarget.PARTNER,
				partner.id(),
				command.categoryId()
			);
		}

		if (command.hashtags() != null) {
			hashtagAssignmentService.replace(HashtagTargetType.PARTNER, partner.id(), command.hashtags());
		}
		if (command.specified("links")) {
			linkAssignmentService.replace(partner, command.linksJson());
		}
		synchronizeMedia(partner, command);
		boolean reviewRestarted = reviewCriticalInformationChanged
			&& partner.restartReviewAfterCriticalInformationChange();
		if (reviewRestarted) {
			partnerRepository.saveAndFlush(partner);
			summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		}

		historyService.record(actor, partner, reviewRestarted ? ACTION_REVIEW_RESTARTED : "UPDATED",
			null, before, historyService.capture(partner));
		return result(actor, ownedPartner(actor, partnerId));
	}

	private boolean reviewCriticalInformationChanged(
		Partner partner,
		UpdatePartnerOnboardingCommand command,
		String normalizedName
	) {
		if (command.specifiedFields().stream().noneMatch(REVIEW_CRITICAL_FIELDS::contains)) {
			return false;
		}
		if (command.specified("name") && !Objects.equals(partner.name(), normalizedName)) {
			return true;
		}
		if (command.specified("category_id") && !categoryAssignmentService.isAssigned(
			CategoryAssignmentTarget.PARTNER,
			partner.id(),
			command.categoryId()
		)) {
			return true;
		}
		if (changed(command, "road_address", partner.roadAddress(), command.roadAddress())
			|| changed(command, "detail_address", partner.detailAddress(), command.detailAddress())
			|| changed(command, "latitude", partner.latitude(), command.latitude())
			|| changed(command, "longitude", partner.longitude(), command.longitude())) {
			return true;
		}
		if (command.contacts() != null) {
			if (changed(
				command,
				"representative_phone",
				contactValue(partner, PartnerContactType.REPRESENTATIVE_PHONE),
				command.contacts().representativePhone()
			) || changed(
				command,
				"representative_email",
				contactValue(partner, PartnerContactType.REPRESENTATIVE_EMAIL),
				command.contacts().representativeEmail()
			)) {
				return true;
			}
		}
		if (command.businessRegistration() != null
			&& businessRegistrationChanged(partner.businessRegistration(), command)) {
			return true;
		}
		return command.businessRegistrationFile() != null;
	}

	private boolean businessRegistrationChanged(
		PartnerBusinessRegistration current,
		UpdatePartnerOnboardingCommand command
	) {
		PartnerBusinessRegistrationCommand registration = command.businessRegistration();
		String currentBusinessNumber = current == null ? null : current.businessNumber();
		String nextBusinessNumber = command.specified("business_number")
			? businessNumberPolicy.normalize(registration.businessNumber())
			: currentBusinessNumber;
		return !Objects.equals(currentBusinessNumber, nextBusinessNumber)
			|| changed(command, "company_name", current == null ? null : current.companyName(), registration.companyName())
			|| changed(command, "ceo_name", current == null ? null : current.ceoName(), registration.ceoName());
	}

	private boolean changed(
		UpdatePartnerOnboardingCommand command,
		String field,
		String current,
		String requested
	) {
		return command.specified(field) && !Objects.equals(trimToNull(current), trimToNull(requested));
	}

	private String contactValue(Partner partner, PartnerContactType type) {
		return partner.contacts().stream()
			.filter(PartnerContact::active)
			.filter(contact -> contact.contactType() == type)
			.sorted(Comparator.comparing(PartnerContact::sortOrder))
			.map(PartnerContact::value)
			.findFirst()
			.orElse(null);
	}

	@Transactional
	public PartnerOnboardingResult submit(AuthenticatedActor actor, Long partnerId) {
		Partner partner = editablePartner(actor, partnerId);
		validateSubmission(partner);
		PartnerAllowStatus before = partner.allowStatus();
		partner.requestReview();
		partnerRepository.saveAndFlush(partner);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_SUBMITTED,
			null,
			null
		).captureActor(actor.name(), actor.loginId());
		history.addChange("allow_status", before.name(), PartnerAllowStatus.REVIEW_REQUESTED.name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return result(actor, partner);
	}

	private Partner ownedPartner(AuthenticatedActor actor, Long partnerId) {
		ownershipPolicy.requirePartnerOwner(actor, partnerId);
		return partnerRepository.findByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
	}

	private Partner editablePartner(AuthenticatedActor actor, Long partnerId) {
		ownershipPolicy.requirePartnerOwner(actor, partnerId);
		Partner partner = partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
		if (partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot edit onboarding information.");
		}
		return partner;
	}

	@Transactional(readOnly = true)
	public List<PartnerFeatureResult> availableFeatures(AuthenticatedActor actor, Long partnerId) {
		ownedPartner(actor, partnerId);
		return featureRepository
			.findByStatusOrderBySortOrderAscIdAsc(PartnerFeatureStatus.ACTIVE)
			.stream()
			.map(feature -> new PartnerFeatureResult(
				feature.id(),
				feature.code(),
				feature.name(),
				feature.sortOrder(),
				feature.status().name()
			))
			.toList();
	}

	private void validateSubmission(Partner partner) {
		List<String> missing = new ArrayList<>();
		if (!StringUtils.hasText(partner.name())) {
			missing.add("name");
		}
		if (categoryAssignmentService.references(CategoryAssignmentTarget.PARTNER, partner.id()).isEmpty()) {
			missing.add("category_id");
		}
		if (!StringUtils.hasText(partner.roadAddress())) {
			missing.add("road_address");
		}
		boolean hasRepresentativePhone = partner.contacts().stream().anyMatch(contact ->
			contact.active()
				&& contact.contactType() == PartnerContactType.REPRESENTATIVE_PHONE
				&& StringUtils.hasText(contact.value()));
		if (!hasRepresentativePhone) {
			missing.add("representative_phone");
		}
		boolean hasRepresentativeEmail = partner.contacts().stream().anyMatch(contact ->
			contact.active()
				&& contact.contactType() == PartnerContactType.REPRESENTATIVE_EMAIL
				&& StringUtils.hasText(contact.value()));
		if (!hasRepresentativeEmail) {
			missing.add("representative_email");
		}
		PartnerBusinessRegistration registration = partner.businessRegistration();
		if (registration == null
			|| !StringUtils.hasText(registration.businessNumber())
			|| !StringUtils.hasText(registration.companyName())
			|| !StringUtils.hasText(registration.ceoName())) {
			missing.add("business_registration");
		}
		if (registration != null && StringUtils.hasText(registration.businessNumber())) {
			businessNumberPolicy.normalize(registration.businessNumber());
		}
		if (registration == null || mediaReadService.primary(
			MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
			registration.id(),
			MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
		) == null) {
			missing.add("business_registration_file");
		}
		if (!missing.isEmpty()) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"Required onboarding information is missing: " + String.join(", ", missing)
			);
		}
	}

	private void synchronizeMedia(Partner partner, UpdatePartnerOnboardingCommand command) {
		if (command.specified("logo") || command.specified("existing_logo_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_LOGO,
				command.logo(),
				command.existingLogoId(),
				false
			);
		}
		if (command.specified("main_image") || command.specified("existing_main_image_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_MAIN_IMAGE,
				command.mainImage(),
				command.existingMainImageId(),
				false
			);
		}
		if (command.specified("interior_image_order")) {
			mediaCommandService.synchronizeManyOrdered(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
				command.interiorImages(),
				command.interiorImageOrder(),
				false,
				9
			);
		} else if (command.specified("interior_images") || command.specified("existing_interior_image_ids")) {
			mediaCommandService.synchronizeMany(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
				command.interiorImages(),
				command.existingInteriorImageIds(),
				false,
				9
			);
		}
		if (command.specified("business_registration_file")
			|| command.specified("existing_business_registration_file_id")) {
			PartnerBusinessRegistration registration = ensureBusinessRegistration(partner);
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE,
				command.businessRegistrationFile(),
				command.existingBusinessRegistrationFileId(),
				false
			);
		}
	}

	private Set<PartnerContact> mergeContacts(
		Partner partner,
		PartnerContactSetCommand command,
		Set<String> fields
	) {
		Map<PartnerContactType, List<String>> values = partner.contacts().stream()
			.filter(PartnerContact::active)
			.sorted(Comparator.comparing(PartnerContact::sortOrder))
			.collect(Collectors.groupingBy(
				PartnerContact::contactType,
				() -> new EnumMap<>(PartnerContactType.class),
				Collectors.mapping(PartnerContact::value, Collectors.toList())
			));
		putSingle(values, PartnerContactType.REPRESENTATIVE_PHONE, command.representativePhone(), fields, "representative_phone");
		putSingle(values, PartnerContactType.REPRESENTATIVE_EMAIL, command.representativeEmail(), fields, "representative_email");
		putSingle(values, PartnerContactType.SMS_SENDER_PHONE, command.smsSenderPhone(), fields, "sms_sender_phone");
		putSingle(values, PartnerContactType.CALL_RECEIVER_PHONE, command.callReceiverPhone(), fields, "call_receiver_phone");
		putMany(values, PartnerContactType.CONSULTATION_RECEIVER_PHONE, command.consultationReceiverPhones(), fields, "consultation_receiver_phones");
		putMany(values, PartnerContactType.EVENT_NOTICE_RECEIVER_PHONE, command.eventNoticeReceiverPhones(), fields, "event_notice_receiver_phones");
		putMany(values, PartnerContactType.NOTICE_MARKETING_EMAIL, command.noticeMarketingEmails(), fields, "notice_marketing_emails");

		Set<PartnerContact> contacts = new LinkedHashSet<>();
		for (PartnerContactType type : PartnerContactType.values()) {
			List<String> typeValues = values.getOrDefault(type, List.of());
			for (int index = 0; index < typeValues.size(); index++) {
				String value = trimToNull(typeValues.get(index));
				if (value != null) {
					contacts.add(new PartnerContact(type, value, index, index == 0));
				}
			}
		}
		return contacts;
	}

	private void putSingle(
		Map<PartnerContactType, List<String>> values,
		PartnerContactType type,
		String value,
		Set<String> fields,
		String field
	) {
		if (fields.contains(field)) {
			String normalized = trimToNull(value);
			values.put(type, normalized == null ? List.of() : List.of(normalized));
		}
	}

	private void putMany(
		Map<PartnerContactType, List<String>> values,
		PartnerContactType type,
		List<String> newValues,
		Set<String> fields,
		String field
	) {
		if (fields.contains(field)) {
			values.put(type, newValues == null ? List.of() : newValues);
		}
	}

	private void updateBusinessRegistration(
		Partner partner,
		PartnerBusinessRegistrationCommand command,
		Set<String> fields
	) {
		PartnerBusinessRegistration current = partner.businessRegistration();
		String businessNumber = fields.contains("business_number")
			? businessNumberPolicy.normalize(command.businessNumber())
			: current == null ? null : current.businessNumber();
		Long currentId = current == null ? null : current.id();
		if (businessNumber != null) {
			businessRegistrationRepository.findByBusinessNumber(businessNumber)
				.filter(found -> currentId == null || !found.id().equals(currentId))
				.ifPresent(found -> {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "Business number is already in use.");
				});
		}
		String companyName = fieldValue(fields, "company_name", command.companyName(), current == null ? null : current.companyName());
		String ceoName = fieldValue(fields, "ceo_name", command.ceoName(), current == null ? null : current.ceoName());
		var openingDate = fields.contains("opening_date")
			? command.openingDate()
			: current == null ? null : current.openingDate();

		if (current == null) {
			current = new PartnerBusinessRegistration(
				businessNumber,
				companyName,
				ceoName,
				openingDate,
				null,
				null,
				null
			);
			partner.replaceBusinessRegistration(current);
		} else {
			current.update(
				businessNumber,
				companyName,
				ceoName,
				openingDate,
				current.settlementBankName(),
				current.settlementAccountNumber(),
				current.settlementAccountHolder()
			);
		}
	}

	private PartnerBusinessRegistration ensureBusinessRegistration(Partner partner) {
		if (partner.businessRegistration() == null) {
			partner.replaceBusinessRegistration(new PartnerBusinessRegistration(
				null, null, null, null, null, null, null
			));
			partnerRepository.saveAndFlush(partner);
		}
		return partner.businessRegistration();
	}

	private String fieldValue(Set<String> fields, String field, String value, String current) {
		return fields.contains(field) ? trimToNull(value) : current;
	}

	private Set<PartnerFeature> loadFeatures(Set<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		List<PartnerFeature> features = featureRepository.findByIdInAndStatus(ids, PartnerFeatureStatus.ACTIVE);
		if (features.size() != ids.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "An unavailable partner feature was selected.");
		}
		return new LinkedHashSet<>(features);
	}

	private PartnerOnboardingResult result(AuthenticatedActor actor, Partner partner) {
		return new PartnerOnboardingResult(
			partner.id(),
			partner.allowStatus().name(),
			partner.status().name(),
			rejectionReason(partner),
			new PartnerOnboardingResult.BasicInformation(
				partner.name(),
				partner.englishName(),
				partner.description(),
				categoryAssignmentService.references(CategoryAssignmentTarget.PARTNER, partner.id()),
				partner.roadAddress(),
				partner.detailAddress(),
				partner.latitude(),
				partner.longitude(),
				fromJson(partner.subwayStations()),
				partner.operatingHoursNotice(),
				fromJson(partner.operationHours()),
				fromJson(partner.holidayPolicy()),
				partner.direction(),
				hashtagAssignmentService.values(HashtagTargetType.PARTNER, partner.id()),
				contactResults(partner.contacts()),
				partnerMedia(partner.id(), mediaReadService.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_LOGO)),
				partnerMedia(partner.id(), mediaReadService.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_MAIN_IMAGE)),
				mediaReadService.list(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE)
					.stream().map(media -> partnerMedia(partner.id(), media)).toList()
			),
			new PartnerOnboardingResult.AdditionalInformation(
				featureResults(partner.features()),
				linkRepository.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id())
					.stream()
					.map(link -> new PartnerLinkResult(link.id(), link.type().name(), link.url(), link.sortOrder()))
					.toList()
			),
			new PartnerOnboardingResult.PriceInformation(optionService.listByPartnerId(partner.id())),
			new PartnerOnboardingResult.VerificationInformation(businessRegistrationResult(partner.id(), partner.businessRegistration()))
		);
	}

	private String rejectionReason(Partner partner) {
		if (partner.allowStatus() != PartnerAllowStatus.REJECTED) {
			return null;
		}
		return operationHistoryRepository
			.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(OperationHistory.TARGET_PARTNER, partner.id())
			.stream()
			.filter(history -> history.changes().stream().anyMatch(change ->
				"allow_status".equals(change.fieldKey())
					&& PartnerAllowStatus.REJECTED.name().equals(change.afterValue())))
			.map(OperationHistory::reason)
			.findFirst()
			.orElse(null);
	}

	private List<PartnerContactResult> contactResults(Set<PartnerContact> contacts) {
		return contacts.stream()
			.filter(PartnerContact::active)
			.sorted(Comparator.comparing(PartnerContact::contactType).thenComparing(PartnerContact::sortOrder))
			.map(contact -> new PartnerContactResult(
				contact.id(),
				contact.contactType().name(),
				contact.value(),
				contact.sortOrder(),
				contact.primary(),
				contact.active()
			))
			.toList();
	}

	private List<PartnerFeatureResult> featureResults(Set<PartnerFeature> features) {
		return features.stream()
			.sorted(Comparator.comparing(PartnerFeature::sortOrder).thenComparing(PartnerFeature::id))
			.map(feature -> new PartnerFeatureResult(
				feature.id(),
				feature.code(),
				feature.name(),
				feature.sortOrder(),
				feature.status().name()
			))
			.toList();
	}

	private PartnerBusinessRegistrationResult businessRegistrationResult(Long partnerId, PartnerBusinessRegistration registration) {
		if (registration == null) {
			return null;
		}
		return new PartnerBusinessRegistrationResult(
			registration.id(),
			registration.businessNumber(),
			registration.companyName(),
			registration.ceoName(),
			registration.openingDate(),
			new PartnerSettlementAccountResult(null, null, null),
			registration.status().name(),
			partnerMedia(partnerId, mediaReadService.primary(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
			))
		);
	}

	private MediaResult partnerMedia(Long partnerId, MediaResult media) {
		return media == null ? null : media.withContentUrl(
			"/api/v1/partner/partners/%d/media/%d/content".formatted(partnerId, media.id())
		);
	}

	private Object fromJson(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("Stored operation hours JSON is invalid.", exception);
		}
	}

	private String requireText(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
