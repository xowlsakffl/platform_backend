package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.hashtag.HashtagAssignmentService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.application.partner.result.PartnerOptionResult;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.hashtag.HashtagTargetType;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerFeature;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerLinkRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerHistoryService {

	private final CategoryAssignmentService categories;
	private final HashtagAssignmentService hashtags;
	private final PartnerLinkRepository links;
	private final MediaReadService media;
	private final OperationHistoryRepository histories;
	private final ObjectMapper objectMapper;

	public PartnerHistoryService(
		CategoryAssignmentService categories,
		HashtagAssignmentService hashtags,
		PartnerLinkRepository links,
		MediaReadService media,
		OperationHistoryRepository histories,
		ObjectMapper objectMapper
	) {
		this.categories = categories;
		this.hashtags = hashtags;
		this.links = links;
		this.media = media;
		this.histories = histories;
		this.objectMapper = objectMapper;
	}

	public Map<String, String> capture(Partner partner) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("name", partner.name());
		values.put("english_name", partner.englishName());
		values.put("description", partner.description());
		values.put("categories", json(categories.references(CategoryAssignmentTarget.PARTNER, partner.id())));
		values.put("road_address", partner.roadAddress());
		values.put("detail_address", partner.detailAddress());
		values.put("latitude", partner.latitude());
		values.put("longitude", partner.longitude());
		values.put("subway_stations", normalizeJson(partner.subwayStations()));
		values.put("operation_hours", normalizeJson(partner.operationHours()));
		values.put("holiday_policy", normalizeJson(partner.holidayPolicy()));
		values.put("operating_hours_notice", partner.operatingHoursNotice());
		values.put("direction", partner.direction());
		values.put("allow_status", partner.allowStatus().name());
		values.put("status", partner.status().name());
		values.put("reviewer_staff_id", partner.reviewerStaff() == null ? null : partner.reviewerStaff().id().toString());
		values.put("review_started_at", partner.reviewStartedAt() == null ? null : partner.reviewStartedAt().toString());
		values.put("hashtags", json(hashtags.values(HashtagTargetType.PARTNER, partner.id())));
		values.put("links", json(links.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id()).stream()
			.map(link -> Map.of("type", link.type().name(), "url", link.url(), "sort_order", link.sortOrder())).toList()));
		values.put("contacts", json(partner.contacts().stream()
			.sorted(Comparator.comparing((PartnerContact contact) -> contact.contactType().name())
				.thenComparingInt(PartnerContact::sortOrder))
			.map(contact -> Map.of("type", contact.contactType().name(), "value", contact.value(),
				"sort_order", contact.sortOrder(), "is_primary", contact.primary(), "is_active", contact.active())).toList()));
		values.put("features", json(partner.features().stream().sorted(Comparator.comparing(PartnerFeature::id))
			.map(feature -> Map.of("id", feature.id(), "code", feature.code(), "name", feature.name())).toList()));
		var registration = partner.businessRegistration();
		if (registration != null) {
			Map<String, Object> business = new LinkedHashMap<>();
			business.put("business_number", registration.businessNumber());
			business.put("company_name", registration.companyName());
			business.put("ceo_name", registration.ceoName());
			business.put("opening_date", registration.openingDate() == null ? null : registration.openingDate().toString());
			business.put("settlement_bank_name", registration.settlementBankName());
			business.put("settlement_account_number", registration.settlementAccountNumber());
			business.put("settlement_account_holder", registration.settlementAccountHolder());
			values.put("business_registration", json(business));
			values.put("business_registration_file", json(mediaValue(media.primary(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION, registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE))));
		}
		values.put("logo", json(mediaValue(media.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_LOGO))));
		values.put("main_image", json(mediaValue(media.primary(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_MAIN_IMAGE))));
		values.put("interior_images", json(media.list(MediaOwnerType.PARTNER, partner.id(), MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE)
			.stream().map(this::mediaValue).toList()));
		return values;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(AuthenticatedActor actor, Partner partner, String action, String reason,
		Map<String, String> before, Map<String, String> after) {
		OperationHistory history = new OperationHistory(OperationHistory.TARGET_PARTNER, partner.id(),
			actor.actorType().name(), actor.accountId(), action, reason, null)
			.captureActor(actor.name(), actor.loginId());
		var keys = new LinkedHashSet<>(before.keySet());
		keys.addAll(after.keySet());
		for (String key : keys) {
			if (!Objects.equals(before.get(key), after.get(key))) {
				history.addChange(key, before.get(key), after.get(key));
			}
		}
		if (!history.changes().isEmpty()) {
			histories.save(history);
		}
	}

	public Map<String, String> options(List<PartnerOptionResult> options) {
		Map<String, String> snapshots = new LinkedHashMap<>();
		for (PartnerOptionResult option : options) {
			Map<String, Object> value = new LinkedHashMap<>();
			value.put("id", option.id());
			value.put("category_id", option.category() == null ? null : option.category().id());
			value.put("name", option.name());
			value.put("description", option.description());
			value.put("regular_price", amount(option.regularPrice()));
			value.put("sale_price", amount(option.salePrice()));
			value.put("duration_minutes", option.durationMinutes());
			value.put("is_visible", option.visible());
			value.put("sort_order", option.sortOrder());
			value.put("specialists", option.specialists().stream()
				.sorted(Comparator.comparing(PartnerOptionResult.SpecialistPriceResult::specialistId))
				.map(specialist -> {
					Map<String, Object> assignment = new LinkedHashMap<>();
					assignment.put("specialist_id", specialist.specialistId());
					assignment.put("regular_price_override", amount(specialist.regularPriceOverride()));
					assignment.put("sale_price_override", amount(specialist.salePriceOverride()));
					return assignment;
				}).toList());
			snapshots.put("option." + option.id(), json(value));
		}
		return snapshots;
	}

	private String amount(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}

	private Object mediaValue(MediaResult value) {
		if (value == null) return null;
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", value.id());
		result.put("name", value.originalName());
		result.put("mime_type", value.mimeType());
		result.put("size", value.size());
		return result;
	}

	private String normalizeJson(String value) {
		if (value == null) return null;
		try {
			return json(objectMapper.readTree(value));
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 업체 정보 JSON이 올바르지 않습니다.", exception);
		}
	}

	private String json(Object value) {
		if (value == null) return null;
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("업체 변경 이력을 만들 수 없습니다.", exception);
		}
	}
}
