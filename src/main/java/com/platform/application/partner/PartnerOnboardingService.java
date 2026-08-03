package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaReadService;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import com.platform.application.partner.command.SignupPartnerOnboardingCommand;
import com.platform.application.partner.command.UpdatePartnerOnboardingCommand;
import com.platform.application.partner.result.PartnerBusinessRegistrationResult;
import com.platform.application.partner.result.PartnerContactResult;
import com.platform.application.partner.result.PartnerFeatureResult;
import com.platform.application.partner.result.PartnerLinkResult;
import com.platform.application.partner.result.PartnerOnboardingResult;
import com.platform.application.partner.result.PartnerOnboardingSignupResult;
import com.platform.application.partner.result.PartnerSettlementAccountResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerBusinessRegistration;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerContactType;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.domain.partner.PartnerHashtag;
import com.platform.domain.partner.PartnerLink;
import com.platform.domain.partner.PartnerLinkType;
import com.platform.domain.partner.PartnerStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import com.platform.infrastructure.persistence.partner.PartnerHashtagRepository;
import com.platform.infrastructure.persistence.partner.PartnerLinkRepository;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerOnboardingService {

	private static final String ACTION_SIGNED_UP = "ONBOARDING_SIGNED_UP";
	private static final String ACTION_SUBMITTED = "ONBOARDING_SUBMITTED";
	private final OwnershipPolicy ownershipPolicy;
	private final PartnerRepository partnerRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final PartnerFeatureRepository featureRepository;
	private final PartnerHashtagRepository hashtagRepository;
	private final PartnerLinkRepository linkRepository;
	private final PartnerOptionRepository optionRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final PartnerOptionForPartnerService optionService;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;
	private final PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper;

	public PartnerOnboardingService(
		OwnershipPolicy ownershipPolicy,
		PartnerRepository partnerRepository,
		AccountPartnerRepository accountPartnerRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		PartnerFeatureRepository featureRepository,
		PartnerHashtagRepository hashtagRepository,
		PartnerLinkRepository linkRepository,
		PartnerOptionRepository optionRepository,
		OperationHistoryRepository operationHistoryRepository,
		PartnerOptionForPartnerService optionService,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		StaffSummaryCacheInvalidator summaryCacheInvalidator,
		PasswordEncoder passwordEncoder,
		ObjectMapper objectMapper
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.partnerRepository = partnerRepository;
		this.accountPartnerRepository = accountPartnerRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.hashtagRepository = hashtagRepository;
		this.linkRepository = linkRepository;
		this.optionRepository = optionRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.optionService = optionService;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
		this.passwordEncoder = passwordEncoder;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public PartnerOnboardingSignupResult signup(SignupPartnerOnboardingCommand command) {
		String partnerName = requireText(command.partnerName(), "Partner name is required.");
		String managerName = requireText(command.managerName(), "Manager name is required.");
		String nickname = requireText(command.nickname(), "Nickname is required.");
		String email = requireText(command.email(), "Email is required.").toLowerCase(Locale.ROOT);
		if (partnerRepository.existsByNameAndDeletedAtIsNull(partnerName)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Partner name is already in use.");
		}
		if (accountPartnerRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Email is already in use.");
		}
		if (accountPartnerRepository.existsByNickname(nickname)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Nickname is already in use.");
		}

		Partner partner = partnerRepository.saveAndFlush(Partner.createDraft(partnerName));
		AccountPartner account = accountPartnerRepository.saveAndFlush(AccountPartner.create(
			partner,
			managerName,
			nickname,
			email,
			trimToNull(command.phone()),
			passwordEncoder.encode(command.password()),
			AccountPartnerStatus.ACTIVE
		));
		operationHistoryRepository.save(new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			"PARTNER",
			account.id(),
			ACTION_SIGNED_UP,
			null,
			null
		));

		return new PartnerOnboardingSignupResult(
			partner.id(),
			account.id(),
			account.email(),
			partner.allowStatus().name()
		);
	}

	@Transactional(readOnly = true)
	public PartnerOnboardingResult get(AuthenticatedActor actor) {
		Partner partner = ownedPartner(actor);
		return result(actor, partner);
	}

	@Transactional
	public PartnerOnboardingResult update(
		AuthenticatedActor actor,
		UpdatePartnerOnboardingCommand command
	) {
		Partner partner = editablePartner(actor);
		String name = command.specified("name")
			? requireText(command.name(), "Partner name cannot be empty.")
			: partner.name();
		if (!Objects.equals(name, partner.name()) && partnerRepository.existsByNameAndDeletedAtIsNull(name)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Partner name is already in use.");
		}

		partner.updateOnboardingProfile(
			name,
			command.specified("description") ? trimToNull(command.description()) : partner.description(),
			command.specified("industry") ? command.industry() : partner.industry(),
			command.specified("road_address") ? trimToNull(command.roadAddress()) : partner.roadAddress(),
			command.specified("jibun_address") ? trimToNull(command.jibunAddress()) : partner.jibunAddress(),
			command.specified("detail_address") ? trimToNull(command.detailAddress()) : partner.detailAddress(),
			command.specified("latitude") ? trimToNull(command.latitude()) : partner.latitude(),
			command.specified("longitude") ? trimToNull(command.longitude()) : partner.longitude(),
			command.specified("operating_hours_notice")
				? trimToNull(command.operatingHoursNotice())
				: partner.operatingHoursNotice(),
			command.specified("operation_hours")
				? normalizeOperationHours(command.operationHours())
				: partner.operationHours(),
			command.specified("direction") ? trimToNull(command.direction()) : partner.direction()
		);

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

		if (command.hashtags() != null) {
			replaceHashtags(partner, command.hashtags());
		}
		if (command.specified("links")) {
			replaceLinks(partner, command.linksJson());
		}
		synchronizeMedia(partner, command);

		return result(actor, ownedPartner(actor));
	}

	@Transactional
	public PartnerOnboardingResult submit(AuthenticatedActor actor) {
		Partner partner = editablePartner(actor);
		validateSubmission(partner);
		PartnerAllowStatus before = partner.allowStatus();
		partner.changeAllowStatus(PartnerAllowStatus.PENDING);
		partnerRepository.saveAndFlush(partner);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_SUBMITTED,
			null,
			null
		);
		history.addChange("allow_status", before.name(), PartnerAllowStatus.PENDING.name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return result(actor, partner);
	}

	private Partner ownedPartner(AuthenticatedActor actor) {
		ownershipPolicy.requirePartnerOwner(actor, actor.partnerId());
		return partnerRepository.findByIdAndDeletedAtIsNull(actor.partnerId())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
	}

	private Partner editablePartner(AuthenticatedActor actor) {
		ownershipPolicy.requirePartnerOwner(actor, actor.partnerId());
		Partner partner = partnerRepository.findForUpdateByIdAndDeletedAtIsNull(actor.partnerId())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
		if (partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot edit onboarding information.");
		}
		if (partner.allowStatus() != PartnerAllowStatus.DRAFT
			&& partner.allowStatus() != PartnerAllowStatus.REJECTED) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"Onboarding information can be changed only in DRAFT or REJECTED status."
			);
		}
		return partner;
	}

	private void validateSubmission(Partner partner) {
		List<String> missing = new ArrayList<>();
		if (!StringUtils.hasText(partner.name())) {
			missing.add("name");
		}
		if (!StringUtils.hasText(partner.description())) {
			missing.add("description");
		}
		if (partner.industry() == null) {
			missing.add("industry");
		}
		if (!StringUtils.hasText(partner.roadAddress())) {
			missing.add("road_address");
		}
		if (!StringUtils.hasText(partner.jibunAddress())) {
			missing.add("jibun_address");
		}
		if (!StringUtils.hasText(partner.latitude()) || !StringUtils.hasText(partner.longitude())) {
			missing.add("coordinates");
		}
		if (!StringUtils.hasText(partner.operationHours())) {
			missing.add("operation_hours");
		}
		boolean hasRepresentativePhone = partner.contacts().stream().anyMatch(contact ->
			contact.active()
				&& contact.contactType() == PartnerContactType.REPRESENTATIVE_PHONE
				&& StringUtils.hasText(contact.value()));
		if (!hasRepresentativePhone) {
			missing.add("representative_phone");
		}
		PartnerBusinessRegistration registration = partner.businessRegistration();
		if (registration == null
			|| !StringUtils.hasText(registration.businessNumber())
			|| !StringUtils.hasText(registration.companyName())
			|| !StringUtils.hasText(registration.ceoName())
			|| !StringUtils.hasText(registration.businessType())
			|| !StringUtils.hasText(registration.businessItem())) {
			missing.add("business_registration");
		}
		if (mediaReadService.primary(
			MediaOwnerType.PARTNER,
			partner.id(),
			MediaCollectionPolicy.PARTNER_MAIN_IMAGE
		) == null) {
			missing.add("main_image");
		}
		if (registration == null || mediaReadService.primary(
			MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
			registration.id(),
			MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
		) == null) {
			missing.add("business_registration_file");
		}
		if (optionRepository.countByPartner_IdAndDeletedAtIsNull(partner.id()) == 0) {
			missing.add("price_option");
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
				10
			);
		} else if (command.specified("interior_images") || command.specified("existing_interior_image_ids")) {
			mediaCommandService.synchronizeMany(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
				command.interiorImages(),
				command.existingInteriorImageIds(),
				false,
				10
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
			? normalizeBusinessNumber(command.businessNumber())
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
		String businessType = fieldValue(fields, "business_type", command.businessType(), current == null ? null : current.businessType());
		String businessItem = fieldValue(fields, "business_item", command.businessItem(), current == null ? null : current.businessItem());
		String businessAddress = fieldValue(fields, "business_address", command.businessAddress(), current == null ? null : current.businessAddress());
		String businessAddressDetail = fieldValue(fields, "business_address_detail", command.businessAddressDetail(), current == null ? null : current.businessAddressDetail());
		String taxInvoiceEmail = fieldValue(fields, "tax_invoice_email", command.taxInvoiceEmail(), current == null ? null : current.taxInvoiceEmail());

		if (current == null) {
			current = new PartnerBusinessRegistration(
				businessNumber,
				companyName,
				ceoName,
				businessType,
				businessItem,
				businessAddress,
				businessAddressDetail,
				null,
				null,
				null,
				taxInvoiceEmail,
				fields.contains("issued_at") ? command.issuedAt() : null
			);
			partner.replaceBusinessRegistration(current);
		} else {
			current.update(
				businessNumber,
				companyName,
				ceoName,
				businessType,
				businessItem,
				businessAddress,
				businessAddressDetail,
				current.settlementBankName(),
				current.settlementAccountNumber(),
				current.settlementAccountHolder(),
				taxInvoiceEmail,
				fields.contains("issued_at") ? command.issuedAt() : current.issuedAt()
			);
		}
	}

	private PartnerBusinessRegistration ensureBusinessRegistration(Partner partner) {
		if (partner.businessRegistration() == null) {
			partner.replaceBusinessRegistration(new PartnerBusinessRegistration(
				null, null, null, null, null, null, null, null, null, null, null, null
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

	private void replaceHashtags(Partner partner, List<String> values) {
		LinkedHashMap<String, String> hashtags = new LinkedHashMap<>();
		for (String rawValue : values) {
			String value = trimToNull(rawValue);
			if (value == null) {
				continue;
			}
			value = value.replaceFirst("^#+", "").trim();
			if (value.isEmpty() || value.length() > 30) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "A hashtag must be between 1 and 30 characters.");
			}
			hashtags.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
		}
		if (hashtags.size() > 10) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Up to 10 hashtags can be saved.");
		}
		hashtagRepository.deleteByPartner_Id(partner.id());
		hashtagRepository.flush();
		int index = 0;
		for (String value : hashtags.values()) {
			hashtagRepository.save(new PartnerHashtag(partner, value, index++));
		}
	}

	private void replaceLinks(Partner partner, String linksJson) {
		List<LinkPayload> payloads = parseLinks(linksJson);
		if (payloads.size() > PartnerLinkType.values().length) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Too many external links were supplied.");
		}
		Set<PartnerLinkType> types = new LinkedHashSet<>();
		for (LinkPayload payload : payloads) {
			if (payload.type() == null || !types.add(payload.type())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "Each external link type can be saved only once.");
			}
			validateUrl(payload.url());
		}
		linkRepository.deleteByPartner_Id(partner.id());
		linkRepository.flush();
		for (int index = 0; index < payloads.size(); index++) {
			LinkPayload payload = payloads.get(index);
			linkRepository.save(new PartnerLink(
				partner,
				payload.type(),
				payload.url().trim(),
				payload.sortOrder() == null ? index : payload.sortOrder()
			));
		}
	}

	private List<LinkPayload> parseLinks(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readerForListOf(LinkPayload.class).readValue(json);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "External links JSON is invalid.");
		}
	}

	private void validateUrl(String value) {
		String normalized = trimToNull(value);
		if (normalized == null || normalized.length() > 1000) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "External link URL is invalid.");
		}
		try {
			URI uri = new URI(normalized);
			if (uri.getHost() == null
				|| uri.getScheme() == null
				|| !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "External links must use HTTP or HTTPS.");
			}
		} catch (URISyntaxException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "External link URL is invalid.");
		}
	}

	private String normalizeOperationHours(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(value);
			if (root == null || !root.isObject()) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "Operation hours must be a JSON object.");
			}
			for (String day : List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun")) {
				JsonNode hours = root.get(day);
				if (hours == null || !hours.isObject() || !hours.path("is_closed").isBoolean()) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "Operation hours are required for every day.");
				}
				if (hours.path("is_closed").booleanValue()) {
					continue;
				}
				String start = hours.path("start").isTextual() ? hours.path("start").textValue() : null;
				String end = hours.path("end").isTextual() ? hours.path("end").textValue() : null;
				if (start == null || end == null || !start.matches("^\\d{2}:\\d{2}$") || !end.matches("^\\d{2}:\\d{2}$")) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "Operation hours must use HH:mm.");
				}
				LocalTime startTime = LocalTime.parse(start);
				LocalTime endTime = LocalTime.parse(end);
				if (!endTime.isAfter(startTime)) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "Closing time must be after opening time.");
				}
			}
			return objectMapper.writeValueAsString(root);
		} catch (ApiException exception) {
			throw exception;
		} catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Operation hours JSON is invalid.");
		}
	}

	private PartnerOnboardingResult result(AuthenticatedActor actor, Partner partner) {
		return new PartnerOnboardingResult(
			partner.id(),
			partner.allowStatus().name(),
			partner.status().name(),
			rejectionReason(partner),
			new PartnerOnboardingResult.BasicInformation(
				partner.name(),
				partner.description(),
				partner.industry() == null ? null : partner.industry().name(),
				partner.industry() == null ? null : partner.industry().label(),
				partner.roadAddress(),
				partner.jibunAddress(),
				partner.detailAddress(),
				partner.latitude(),
				partner.longitude(),
				partner.operatingHoursNotice(),
				fromJson(partner.operationHours()),
				partner.direction(),
				hashtagRepository.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id())
					.stream().map(PartnerHashtag::value).toList(),
				contactResults(partner.contacts()),
				mediaReadService.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_LOGO),
				mediaReadService.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_MAIN_IMAGE),
				mediaReadService.list(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE)
			),
			new PartnerOnboardingResult.AdditionalInformation(
				featureResults(partner.features()),
				linkRepository.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id())
					.stream()
					.map(link -> new PartnerLinkResult(link.id(), link.type().name(), link.url(), link.sortOrder()))
					.toList()
			),
			new PartnerOnboardingResult.PriceInformation(optionService.list(actor)),
			new PartnerOnboardingResult.VerificationInformation(businessRegistrationResult(partner.businessRegistration()))
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

	private PartnerBusinessRegistrationResult businessRegistrationResult(PartnerBusinessRegistration registration) {
		if (registration == null) {
			return null;
		}
		return new PartnerBusinessRegistrationResult(
			registration.id(),
			registration.businessNumber(),
			registration.companyName(),
			registration.ceoName(),
			registration.businessType(),
			registration.businessItem(),
			registration.businessAddress(),
			registration.businessAddressDetail(),
			new PartnerSettlementAccountResult(null, null, null, registration.taxInvoiceEmail()),
			registration.issuedAt(),
			registration.status().name(),
			mediaReadService.primary(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
			)
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

	private String normalizeBusinessNumber(String value) {
		String normalized = trimToNull(value);
		return normalized == null ? null : normalized.replaceAll("\\D+", "");
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

	private record LinkPayload(
		PartnerLinkType type,
		String url,
		@JsonProperty("sort_order") Integer sortOrder
	) {
	}
}
