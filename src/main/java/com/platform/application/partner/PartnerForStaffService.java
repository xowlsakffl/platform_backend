package com.platform.application.partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.auth.AuthSessionService;
import com.platform.application.auth.PermissionService;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.category.result.CategoryReferenceResult;
import com.platform.application.specialist.SpecialistLifecycleService;
import com.platform.application.specialist.result.SpecialistFieldResult;
import com.platform.application.partner.command.CreatePartnerCommand;
import com.platform.application.partner.command.ChangePartnerAccountStatusCommand;
import com.platform.application.partner.command.ChangePartnerAllowStatusCommand;
import com.platform.application.partner.command.ChangePartnerStatusCommand;
import com.platform.application.partner.command.PartnerBusinessRegistrationCommand;
import com.platform.application.partner.command.PartnerContactSetCommand;
import com.platform.application.partner.command.UpdatePartnerCommand;
import com.platform.application.partner.query.SearchPartnersQuery;
import com.platform.application.partner.query.GetPartnerForStaffQuery;
import com.platform.application.partner.query.SearchPartnerOperationHistoriesForStaffQuery;
import com.platform.application.partner.result.DuplicateCheckResult;
import com.platform.application.partner.result.PartnerAccountResult;
import com.platform.application.partner.result.PartnerAccountInvitationResult;
import com.platform.application.partner.result.PartnerAssignedStaffResult;
import com.platform.application.partner.result.PartnerAllowStatusBulkUpdateResult;
import com.platform.application.partner.result.PartnerBusinessRegistrationResult;
import com.platform.application.partner.result.PartnerContactGroupResult;
import com.platform.application.partner.result.PartnerContactResult;
import com.platform.application.partner.result.PartnerDeletedResult;
import com.platform.application.partner.result.PartnerDetailResult;
import com.platform.application.partner.result.PartnerSpecialistForStaffResult;
import com.platform.application.partner.result.PartnerFeatureResult;
import com.platform.application.partner.result.PartnerListItemResult;
import com.platform.application.partner.result.PartnerLinkResult;
import com.platform.application.partner.result.PartnerSummaryResult;
import com.platform.application.partner.result.PartnerSettlementAccountResult;
import com.platform.application.partner.result.OperationHistoryChangeResult;
import com.platform.application.partner.result.OperationHistoryResult;
import com.platform.application.media.MediaLifecycleService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.AccessPermissions;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.account.AccountPartnerStatus;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.category.CategoryAssignment;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerAccountInvitation;
import com.platform.domain.partner.PartnerAccountLinkStatus;
import com.platform.domain.partner.PartnerBusinessRegistration;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerContactType;
import com.platform.domain.partner.PartnerFeature;
import com.platform.domain.partner.PartnerFeatureStatus;
import com.platform.domain.partner.PartnerHashtag;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import com.platform.infrastructure.persistence.partner.PartnerAccountInvitationRepository;
import com.platform.infrastructure.persistence.partner.PartnerFeatureRepository;
import com.platform.infrastructure.persistence.partner.PartnerHashtagRepository;
import com.platform.infrastructure.persistence.partner.PartnerLinkRepository;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.partner.PartnerResourceCount;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerForStaffService {

	private static final String ACTION_CREATED = "ACTION_CREATED";
	private static final String ACTION_UPDATED = "ACTION_UPDATED";
	private static final String ACTION_STATUS_UPDATED = "ACTION_STATUS_UPDATED";
	private static final String ACTION_ALLOW_STATUS_UPDATED = "ACTION_ALLOW_STATUS_UPDATED";
	private static final String ACTION_ACCOUNT_STATUS_UPDATED = "ACTION_ACCOUNT_STATUS_UPDATED";
	private static final String ACTION_ASSIGNED_STAFF_UPDATED = "ACTION_ASSIGNED_STAFF_UPDATED";
	private static final String ACTION_DELETED = "ACTION_DELETED";
	private final PermissionService permissionService;
	private final AuthSessionService authSessionService;
	private final CategoryAssignmentService categoryAssignmentService;
	private final PartnerRepository partnerRepository;
	private final AccountPartnerRepository accountPartnerRepository;
	private final AccountStaffRepository accountStaffRepository;
	private final PartnerAccountInvitationRepository invitationRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final PartnerFeatureRepository featureRepository;
	private final PartnerHashtagRepository hashtagRepository;
	private final PartnerLinkRepository linkRepository;
	private final PartnerOptionRepository partnerOptionRepository;
	private final PartnerOptionForPartnerService optionService;
	private final OperationHistoryRepository operationHistoryRepository;
	private final MediaLifecycleService mediaLifecycleService;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final SpecialistLifecycleService specialistLifecycleService;
	private final SpecialistRepository specialistRepository;
	private final StaffSummaryCache staffSummaryCache;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;
	private final ObjectMapper objectMapper;

	public PartnerForStaffService(
		PermissionService permissionService,
		AuthSessionService authSessionService,
		CategoryAssignmentService categoryAssignmentService,
		PartnerRepository partnerRepository,
		AccountPartnerRepository accountPartnerRepository,
		AccountStaffRepository accountStaffRepository,
		PartnerAccountInvitationRepository invitationRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		PartnerFeatureRepository featureRepository,
		PartnerHashtagRepository hashtagRepository,
		PartnerLinkRepository linkRepository,
		PartnerOptionRepository partnerOptionRepository,
		PartnerOptionForPartnerService optionService,
		OperationHistoryRepository operationHistoryRepository,
		MediaLifecycleService mediaLifecycleService,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		SpecialistLifecycleService specialistLifecycleService,
		SpecialistRepository specialistRepository,
		StaffSummaryCache staffSummaryCache,
		StaffSummaryCacheInvalidator summaryCacheInvalidator,
		ObjectMapper objectMapper
	) {
		this.permissionService = permissionService;
		this.authSessionService = authSessionService;
		this.categoryAssignmentService = categoryAssignmentService;
		this.partnerRepository = partnerRepository;
		this.accountPartnerRepository = accountPartnerRepository;
		this.accountStaffRepository = accountStaffRepository;
		this.invitationRepository = invitationRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.hashtagRepository = hashtagRepository;
		this.linkRepository = linkRepository;
		this.partnerOptionRepository = partnerOptionRepository;
		this.optionService = optionService;
		this.operationHistoryRepository = operationHistoryRepository;
		this.mediaLifecycleService = mediaLifecycleService;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.specialistLifecycleService = specialistLifecycleService;
		this.specialistRepository = specialistRepository;
		this.staffSummaryCache = staffSummaryCache;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<PartnerListItemResult> list(AuthenticatedActor actor, SearchPartnersQuery condition) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			clamp(condition.perPage(), 1, 100),
			sort(condition)
		);
		Page<Partner> page = partnerRepository.findAll(specification(condition), pageable);
		List<Long> partnerIds = page.getContent().stream().map(Partner::id).toList();
		Map<Long, AccountPartner> accounts = accountsByPartnerIds(page.getContent());
		Map<Long, PartnerAccountInvitation> invitations = latestInvitationsByPartnerIds(page.getContent());
		Map<Long, List<CategoryReferenceResult>> categories = categoryAssignmentService.referencesByTargetIds(
			CategoryAssignmentTarget.PARTNER,
			partnerIds
		);
		Map<Long, Long> specialistCounts = resourceCounts(
			partnerIds.isEmpty() ? List.of() : specialistRepository.countActiveByPartnerIds(partnerIds)
		);
		Map<Long, Long> optionCounts = resourceCounts(
			partnerIds.isEmpty() ? List.of() : partnerOptionRepository.countActiveByPartnerIds(partnerIds)
		);
		Map<Long, MediaResult> logos = mediaReadService.primaries(
			MediaOwnerType.PARTNER,
			page.getContent().stream().map(Partner::id).collect(Collectors.toSet()),
			MediaCollectionPolicy.PARTNER_LOGO
		);

		return PaginatedResponse.from(page, partner -> toListItem(
			partner,
			accounts.get(partner.id()),
			logos.get(partner.id()),
			categories.getOrDefault(partner.id(), List.of()),
			specialistCounts.getOrDefault(partner.id(), 0L),
			optionCounts.getOrDefault(partner.id(), 0L),
			invitations.get(partner.id())
		));
	}

	@Transactional(readOnly = true)
	public PartnerSummaryResult summary(AuthenticatedActor actor) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		return staffSummaryCache.remember(StaffSummaryCache.PARTNER, PartnerSummaryResult.class, () ->
			new PartnerSummaryResult(
				accountPartnerRepository.countDormantPartnerAccounts(LocalDateTime.now().minusDays(30)),
				partnerRepository.countByAllowStatus(PartnerAllowStatus.PENDING),
				partnerRepository.countByAllowStatus(PartnerAllowStatus.REJECTED),
				partnerRepository.countByDeletedAtIsNullAndStatus(PartnerStatus.SUSPENDED),
				partnerRepository.countWithdrawnOrDeleted()
			)
		);
	}

	@Transactional(readOnly = true)
	public List<PartnerAssignedStaffResult> assignedStaffOptions(AuthenticatedActor actor, String query) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ASSIGN_STAFF);
		String normalizedQuery = trimToNull(query);
		String searchPattern = normalizedQuery == null
			? null
			: "%" + normalizedQuery.toLowerCase(Locale.ROOT) + "%";
		return accountStaffRepository.searchActiveOptions(searchPattern)
			.stream()
			.map(this::assignedStaffResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public PartnerDetailResult get(AuthenticatedActor actor, Long id, GetPartnerForStaffQuery query) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		if (query.includes("specialists")) {
			permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_SHOW);
		}
		Partner partner = findActivePartner(id);
		return toDetail(partner, query.include());
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<OperationHistoryResult> histories(
		AuthenticatedActor actor,
		Long id,
		SearchPartnerOperationHistoriesForStaffQuery condition
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		findActivePartner(id);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			clamp(condition.perPage(), 1, 50),
			Sort.by(Sort.Direction.DESC, "createdAt", "id")
		);
		Page<OperationHistory> page = operationHistoryRepository.findByTargetTypeAndTargetId(
			OperationHistory.TARGET_PARTNER,
			id,
			pageable
		);
		return PaginatedResponse.from(page, this::operationHistoryResult);
	}

	@Transactional
	public PartnerDetailResult create(AuthenticatedActor actor, CreatePartnerCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_CREATE);
		if (partnerRepository.existsByNameAndDeletedAtIsNull(command.name())) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 파트너명입니다.");
		}
		PartnerBusinessRegistrationCommand businessCommand = requireBusinessRegistration(command.businessRegistration());
		String businessNumber = normalizeBusinessNumber(businessCommand.businessNumber());
		if (businessRegistrationRepository.existsByBusinessNumber(businessNumber)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}

		Partner partner = new Partner(
			trim(command.name()),
			trimToNull(command.description()),
			trimToNull(command.roadAddress()),
			trimToNull(command.jibunAddress()),
			trimToNull(command.latitude()),
			trimToNull(command.longitude()),
			trimToNull(command.operatingHoursNotice()),
			normalizeOperationHours(command.operationHours()),
			trimToNull(command.direction()),
			command.allowStatus(),
			command.status()
		);
		partner.changeAccountInvitationEmail(normalizeEmail(command.accountInvitationEmail()));
		partner.changeDetailAddress(trimToNull(command.detailAddress()));
		partner.markStaffCreated(actor.accountId());
		partner.replaceContacts(buildContacts(requireContacts(command.contacts()), true));
		partner.replaceBusinessRegistration(toBusinessRegistration(businessCommand, businessNumber));
		partner.replaceFeatures(loadFeatures(command.featureIds()));

		Partner saved = partnerRepository.saveAndFlush(partner);
		categoryAssignmentService.replacePrimary(
			CategoryAssignmentTarget.PARTNER,
			saved.id(),
			command.categoryId()
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.PARTNER,
			saved.id(),
			MediaCollectionPolicy.PARTNER_LOGO,
			command.logo(),
			null,
			true
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.PARTNER,
			saved.id(),
			MediaCollectionPolicy.PARTNER_MAIN_IMAGE,
			command.mainImage(),
			null,
			true
		);
		mediaCommandService.synchronizeMany(
			MediaOwnerType.PARTNER,
			saved.id(),
			MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
			command.interiorImages(),
			List.of(),
			true,
			5
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
			saved.businessRegistration().id(),
			MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE,
			command.businessRegistrationFile(),
			null,
			true
		);
		recordSimpleHistory(actor, saved, ACTION_CREATED, null);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);

		return toDetail(findActivePartner(saved.id()));
	}

	@Transactional
	public PartnerDetailResult update(AuthenticatedActor actor, Long id, UpdatePartnerCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		if (command.specified("allow_status")) {
			permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ALLOW_STATUS_UPDATE);
		}
		if (command.specified("status")) {
			permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_STATUS_UPDATE);
		}
		Partner partner = findActivePartner(id);
		Map<String, String> before = capture(partner);
		PartnerStatus statusBeforeUpdate = partner.status();
		if (command.specified("allow_status") && command.allowStatus() != null) {
			assertPartnerAllowStatusTransition(partner.allowStatus(), command.allowStatus());
		}
		if (command.specified("status")) {
			assertPartnerStatusTransition(statusBeforeUpdate, command.status());
		}
		if (command.specified("account_invitation_email")) {
			partner.changeAccountInvitationEmail(normalizeEmail(command.accountInvitationEmail()));
		}

		if (command.contacts() != null) {
			partner.replaceContacts(buildContacts(
				mergeContacts(partner, command.contacts(), command.specifiedFields()),
				true
			));
		}
		if (command.businessRegistration() != null) {
			PartnerBusinessRegistrationCommand businessCommand = command.businessRegistration();
			String businessNumber = command.specified("business_number")
				? normalizeBusinessNumber(businessCommand.businessNumber())
				: partner.businessRegistration().businessNumber();
			assertBusinessNumberAvailableForUpdate(partner, businessNumber);
			applyBusinessRegistration(partner, businessCommand, businessNumber, command.specifiedFields());
		}
		if (command.featureIds() != null) {
			partner.replaceFeatures(loadFeatures(command.featureIds()));
		}

		partner.updateProfile(
			command.specified("description") ? trimToNull(command.description()) : partner.description(),
			command.specified("road_address") ? trimToNull(command.roadAddress()) : partner.roadAddress(),
			command.specified("jibun_address") ? trimToNull(command.jibunAddress()) : partner.jibunAddress(),
			command.specified("latitude") ? trimToNull(command.latitude()) : partner.latitude(),
			command.specified("longitude") ? trimToNull(command.longitude()) : partner.longitude(),
			command.specified("operating_hours_notice") ? trimToNull(command.operatingHoursNotice()) : partner.operatingHoursNotice(),
			command.specified("operation_hours")
				? normalizeOperationHours(command.operationHours())
				: partner.operationHours(),
			command.specified("direction") ? trimToNull(command.direction()) : partner.direction(),
			command.allowStatus(),
			command.status()
		);
		Partner saved = partnerRepository.saveAndFlush(partner);
		revokePartnerAccountSessionsWhenWithdrawn(saved, statusBeforeUpdate);

		if (command.specified("category_id")) {
			optionService.validatePartnerCategoryChange(saved.id(), command.categoryId());
			categoryAssignmentService.replacePrimary(
				CategoryAssignmentTarget.PARTNER,
				saved.id(),
				command.categoryId()
			);
		}
		if (command.specified("detail_address")) {
			saved.changeDetailAddress(trimToNull(command.detailAddress()));
		}
		partnerRepository.saveAndFlush(saved);
		if (command.specified("logo") || command.specified("existing_logo_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER,
				saved.id(),
				MediaCollectionPolicy.PARTNER_LOGO,
				command.logo(),
				command.existingLogoId(),
				false
			);
		}
		if (command.specified("main_image") || command.specified("existing_main_image_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER,
				saved.id(),
				MediaCollectionPolicy.PARTNER_MAIN_IMAGE,
				command.mainImage(),
				command.existingMainImageId(),
				false
			);
		}
		if (command.specified("interior_image_order")) {
			mediaCommandService.synchronizeManyOrdered(
				MediaOwnerType.PARTNER,
				saved.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
				command.interiorImages(),
				command.interiorImageOrder(),
				false,
				5
			);
		} else if (command.specified("interior_images") || command.specified("existing_interior_image_ids")) {
			mediaCommandService.synchronizeMany(
				MediaOwnerType.PARTNER,
				saved.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE,
				command.interiorImages(),
				command.existingInteriorImageIds(),
				false,
				5
			);
		}
		if (command.specified("business_registration_file")
			|| command.specified("existing_business_registration_file_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				saved.businessRegistration().id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE,
				command.businessRegistrationFile(),
				command.existingBusinessRegistrationFileId(),
				false
			);
		}
		recordChangedHistory(actor, saved, ACTION_UPDATED, null, before, capture(saved));
		if (command.specified("allow_status") || command.specified("status")) {
			summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		}

		return toDetail(findActivePartner(saved.id()));
	}

	@Transactional
	public PartnerDetailResult changeStatus(AuthenticatedActor actor, Long id, ChangePartnerStatusCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_STATUS_UPDATE);
		Partner partner = findLockedActivePartner(id);
		PartnerStatus before = partner.status();
		assertPartnerStatusTransition(before, command.status());
		if (before == command.status()) {
			return toDetail(partner);
		}
		partner.changeStatus(command.status());
		Partner saved = partnerRepository.saveAndFlush(partner);
		revokePartnerAccountSessionsWhenWithdrawn(saved, before);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			saved.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_STATUS_UPDATED,
			trimToNull(command.reason()),
			null
		);
		history.addChange("status", before.name(), command.status().name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);

		return toDetail(findActivePartner(saved.id()));
	}

	@Transactional
	public PartnerAccountResult changeAccountStatus(
		AuthenticatedActor actor,
		Long id,
		ChangePartnerAccountStatusCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ACCOUNT_STATUS_UPDATE);
		findActivePartner(id);
		AccountPartner account = accountPartnerRepository.findForUpdateByPartner_IdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너 관리자 계정을 찾을 수 없습니다."));
		AccountPartnerStatus before = account.status();
		if (before == command.status()) {
			return accountResponse(account);
		}

		account.changeStatus(command.status());
		AccountPartner saved = accountPartnerRepository.saveAndFlush(account);
		if (saved.status() == AccountPartnerStatus.BLOCKED) {
			authSessionService.revokeAll(AccountActorType.PARTNER, saved.id(), "PARTNER_ACCOUNT_BLOCKED");
		}

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			id,
			actor.actorType().name(),
			actor.accountId(),
			ACTION_ACCOUNT_STATUS_UPDATED,
			trimToNull(command.reason()),
			null
		);
		history.addChange("account_status", before.name(), saved.status().name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return accountResponse(saved);
	}

	@Transactional
	public PartnerAssignedStaffResult changeAssignedStaff(
		AuthenticatedActor actor,
		Long id,
		Long assignedStaffId
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		Partner partner = findLockedActivePartner(id);
		AccountStaff before = partner.assignedStaff();
		Long beforeStaffId = staffId(before);

		if (Objects.equals(beforeStaffId, assignedStaffId)) {
			return assignedStaffResponse(before);
		}

		boolean canAssignAnyStaff = permissionService.hasStaffPermission(
			actor,
			AccessPermissions.PARTNER_ASSIGN_STAFF
		);
		boolean selfAssignment = beforeStaffId == null && Objects.equals(assignedStaffId, actor.accountId());
		boolean selfRelease = Objects.equals(beforeStaffId, actor.accountId()) && assignedStaffId == null;
		if (!canAssignAnyStaff && !selfAssignment && !selfRelease) {
			throw new ApiException(
				ErrorCode.FORBIDDEN,
				"다른 직원을 담당자로 지정하거나 해제할 권한이 없습니다."
			);
		}

		AccountStaff assignedStaff = assignedStaffId == null
			? null
			: accountStaffRepository.findByIdAndDeletedAtIsNull(assignedStaffId)
				.filter(AccountStaff::isActive)
				.orElseThrow(() -> new ApiException(
					ErrorCode.INVALID_REQUEST,
					"선택한 담당 직원이 없거나 활성 상태가 아닙니다."
				));

		partner.assignStaff(assignedStaff);
		Partner saved = partnerRepository.saveAndFlush(partner);
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			saved.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_ASSIGNED_STAFF_UPDATED,
			null,
			null
		);
		history.addChange("assigned_staff_id", stringValue(staffId(before)), stringValue(staffId(assignedStaff)));
		operationHistoryRepository.save(history);
		return assignedStaffResponse(assignedStaff);
	}

	@Transactional
	public PartnerAllowStatusBulkUpdateResult changeAllowStatus(
		AuthenticatedActor actor,
		ChangePartnerAllowStatusCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ALLOW_STATUS_UPDATE);
		requireRejectionReason(command.allowStatus(), command.reason());
		List<Long> normalizedIds = command.ids().stream()
			.filter(Objects::nonNull)
			.filter(id -> id > 0)
			.distinct()
			.toList();
		if (normalizedIds.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 파트너을 선택해주세요.");
		}
		List<Partner> partners = partnerRepository.findByIdInAndDeletedAtIsNull(normalizedIds);
		if (partners.isEmpty()) {
			throw new ApiException(ErrorCode.NOT_FOUND, "변경할 파트너을 찾을 수 없습니다.");
		}

		int updatedCount = 0;
		for (Partner partner : partners) {
			PartnerAllowStatus before = partner.allowStatus();
			if (before == command.allowStatus()) {
				continue;
			}
			assertPartnerAllowStatusTransition(before, command.allowStatus());
			partner.changeAllowStatus(command.allowStatus());
			OperationHistory history = new OperationHistory(
				OperationHistory.TARGET_PARTNER,
				partner.id(),
				actor.actorType().name(),
				actor.accountId(),
				ACTION_ALLOW_STATUS_UPDATED,
				trimToNull(command.reason()),
				null
			);
			history.addChange("allow_status", before.name(), command.allowStatus().name());
			operationHistoryRepository.save(history);
			updatedCount++;
		}
		partnerRepository.saveAll(partners);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);

		return new PartnerAllowStatusBulkUpdateResult(
			updatedCount,
			command.allowStatus().name(),
			partners.stream().map(Partner::id).toList()
		);
	}

	@Transactional
	public PartnerDetailResult reviewAllowStatus(
		AuthenticatedActor actor,
		Long id,
		PartnerAllowStatus allowStatus,
		String reason
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_ALLOW_STATUS_UPDATE);
		requireRejectionReason(allowStatus, reason);
		Partner partner = findLockedActivePartner(id);
		PartnerAllowStatus before = partner.allowStatus();
		assertPartnerAllowStatusTransition(before, allowStatus);
		if (before == allowStatus) {
			return toDetail(partner);
		}
		partner.changeAllowStatus(allowStatus);
		Partner saved = partnerRepository.saveAndFlush(partner);
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			saved.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_ALLOW_STATUS_UPDATED,
			trimToNull(reason),
			null
		);
		history.addChange("allow_status", before.name(), allowStatus.name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return toDetail(findActivePartner(saved.id()));
	}

	@Transactional
	public PartnerDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_DELETE);
		Partner partner = findLockedActivePartner(id);
		specialistLifecycleService.softDeleteByPartner(partner.id());
		if (partner.businessRegistration() != null) {
			mediaLifecycleService.softDeleteOwnedMedia(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				partner.businessRegistration().id()
			);
		}
		partner.softDelete();
		revokePartnerAccountSessions(partner, "PARTNER_DELETED");
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.PARTNER, partner.id());
		Partner saved = partnerRepository.saveAndFlush(partner);
		recordSimpleHistory(actor, saved, ACTION_DELETED, null);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return new PartnerDeletedResult(saved.id(), saved.deletedAt());
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkName(AuthenticatedActor actor, String name) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_CREATE);
		return new DuplicateCheckResult(partnerRepository.existsByNameAndDeletedAtIsNull(trim(name)));
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkBusinessNumber(AuthenticatedActor actor, String businessNumber) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_CREATE);
		return new DuplicateCheckResult(
			businessRegistrationRepository.existsByBusinessNumber(normalizeBusinessNumber(businessNumber))
		);
	}

	private Specification<Partner> specification(SearchPartnersQuery condition) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

			String q = trimToNull(condition.q());
			if (q != null) {
				var accountJoin = root.join("accountPartner", JoinType.LEFT);
				List<Predicate> searchPredicates = new ArrayList<>();
				searchPredicates.add(criteriaBuilder.like(root.get("name"), "%" + q + "%"));
				searchPredicates.add(criteriaBuilder.like(accountJoin.get("nickname"), "%" + q + "%"));
				parseLong(q).ifPresent(value -> searchPredicates.add(criteriaBuilder.equal(root.get("id"), value)));
				predicates.add(criteriaBuilder.or(searchPredicates.toArray(Predicate[]::new)));
			}
			if (condition.status() != null && !condition.status().isEmpty()) {
				predicates.add(root.get("status").in(condition.status()));
			}
			if (condition.accountStatus() != null && !condition.accountStatus().isEmpty()) {
				var accountJoin = root.join("accountPartner", JoinType.INNER);
				predicates.add(accountJoin.get("status").in(condition.accountStatus()));
			}
			if (condition.allowStatus() != null && !condition.allowStatus().isEmpty()) {
				predicates.add(root.get("allowStatus").in(condition.allowStatus()));
			} else {
				predicates.add(criteriaBuilder.notEqual(root.get("allowStatus"), PartnerAllowStatus.DRAFT));
			}
			if (condition.categoryIds() != null && !condition.categoryIds().isEmpty()) {
				var categorySubquery = query.subquery(Long.class);
				var assignment = categorySubquery.from(CategoryAssignment.class);
				categorySubquery.select(assignment.get("id")).where(
					criteriaBuilder.equal(
						assignment.get("categorizableType"),
						CategoryAssignmentTarget.PARTNER.code()
					),
					criteriaBuilder.equal(assignment.get("categorizableId"), root.get("id")),
					assignment.get("category").get("id").in(condition.categoryIds())
				);
				predicates.add(criteriaBuilder.exists(categorySubquery));
			}
			if (condition.registrationSources() != null && !condition.registrationSources().isEmpty()) {
				predicates.add(root.get("registrationSource").in(condition.registrationSources()));
			}
			if (Boolean.TRUE.equals(condition.dormant())) {
				var accountJoin = root.join("accountPartner", JoinType.INNER);
				predicates.add(criteriaBuilder.notEqual(root.get("status"), PartnerStatus.WITHDRAWN));
				predicates.add(criteriaBuilder.isNull(accountJoin.get("deletedAt")));
				predicates.add(criteriaBuilder.equal(accountJoin.get("status"), AccountPartnerStatus.ACTIVE));
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.isNull(accountJoin.get("lastLoginAt")),
					criteriaBuilder.lessThan(accountJoin.<LocalDateTime>get("lastLoginAt"), LocalDateTime.now().minusDays(30))
				));
			}
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("createdAt"), condition.startDate(), condition.endDate());
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("updatedAt"), condition.updatedStartDate(), condition.updatedEndDate());
			if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType()) && !StringUtils.hasText(condition.sort())) {
				query.orderBy(
					criteriaBuilder.asc(
						criteriaBuilder.<Integer>selectCase()
							.when(criteriaBuilder.equal(root.get("allowStatus"), PartnerAllowStatus.PENDING), 0)
							.otherwise(1)
					),
					criteriaBuilder.desc(root.get("createdAt")),
					criteriaBuilder.desc(root.get("id"))
				);
			}
			query.distinct(true);
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchPartnersQuery condition) {
		String sort = trimToNull(condition.sort());
		if (sort == null) {
			return Sort.unsorted();
		}
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return switch (sort) {
			case "id" -> Sort.by(direction, "id");
			case "name" -> Sort.by(direction, "name");
			case "region" -> Sort.by(direction, "regionSortKey").and(Sort.by(direction, "id"));
			case "created_at" -> Sort.by(direction, "createdAt");
			case "status" -> Sort.by(direction, "status");
			case "allow_status" -> Sort.by(direction, "allowStatus");
			default -> Sort.unsorted();
		};
	}

	private void applyDateRange(
		List<Predicate> predicates,
		jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
		jakarta.persistence.criteria.Path<LocalDateTime> path,
		String startDate,
		String endDate
	) {
		if (StringUtils.hasText(startDate)) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, parseDate(startDate).atStartOfDay()));
		}
		if (StringUtils.hasText(endDate)) {
			predicates.add(criteriaBuilder.lessThan(path, parseDate(endDate).plusDays(1).atStartOfDay()));
		}
	}

	private Partner findActivePartner(Long id) {
		return partnerRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너을 찾을 수 없습니다."));
	}

	private Partner findLockedActivePartner(Long id) {
		return partnerRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너을 찾을 수 없습니다."));
	}

	private Set<PartnerContact> buildContacts(PartnerContactSetCommand contacts, boolean requireRepresentative) {
		Map<PartnerContactType, List<String>> values = new LinkedHashMap<>();
		putSingle(values, PartnerContactType.REPRESENTATIVE_PHONE, contacts.representativePhone());
		putSingle(values, PartnerContactType.SMS_SENDER_PHONE, contacts.smsSenderPhone());
		putSingle(values, PartnerContactType.CALL_RECEIVER_PHONE, contacts.callReceiverPhone());
		putMany(values, PartnerContactType.CONSULTATION_RECEIVER_PHONE, contacts.consultationReceiverPhones());
		putMany(values, PartnerContactType.EVENT_NOTICE_RECEIVER_PHONE, contacts.eventNoticeReceiverPhones());
		putMany(values, PartnerContactType.NOTICE_MARKETING_EMAIL, contacts.noticeMarketingEmails());

		if (requireRepresentative && values.getOrDefault(PartnerContactType.REPRESENTATIVE_PHONE, List.of()).isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표 번호는 필수입니다.");
		}

		Set<PartnerContact> result = new LinkedHashSet<>();
		for (Map.Entry<PartnerContactType, List<String>> entry : values.entrySet()) {
			PartnerContactType type = entry.getKey();
			List<String> typedValues = entry.getValue();
			if (typedValues.size() > type.maxCount()) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "연락처 최대 개수를 초과했습니다.", Map.of(
					"type", type.name(),
					"max_count", type.maxCount()
				));
			}
			for (int index = 0; index < typedValues.size(); index++) {
				result.add(new PartnerContact(type, typedValues.get(index), index, index == 0));
			}
		}
		return result;
	}

	private PartnerContactSetCommand mergeContacts(
		Partner partner,
		PartnerContactSetCommand requested,
		Set<String> specifiedFields
	) {
		PartnerContactGroupResult current = contacts(partner);
		return new PartnerContactSetCommand(
			specifiedFields.contains("representative_phone") ? requested.representativePhone() : current.representativePhone(),
			specifiedFields.contains("sms_sender_phone") ? requested.smsSenderPhone() : current.smsSenderPhone(),
			specifiedFields.contains("call_receiver_phone") ? requested.callReceiverPhone() : current.callReceiverPhone(),
			specifiedFields.contains("consultation_receiver_phones")
				? requested.consultationReceiverPhones()
				: current.consultationReceiverPhones(),
			specifiedFields.contains("event_notice_receiver_phones")
				? requested.eventNoticeReceiverPhones()
				: current.eventNoticeReceiverPhones(),
			specifiedFields.contains("notice_marketing_emails")
				? requested.noticeMarketingEmails()
				: current.noticeMarketingEmails()
		);
	}

	private void putSingle(Map<PartnerContactType, List<String>> values, PartnerContactType type, String value) {
		String trimmed = trimToNull(value);
		values.put(type, trimmed == null ? List.of() : List.of(trimmed));
	}

	private void putMany(Map<PartnerContactType, List<String>> values, PartnerContactType type, List<String> rawValues) {
		if (rawValues == null) {
			values.put(type, List.of());
			return;
		}
		values.put(type, rawValues.stream()
			.map(this::trimToNull)
			.filter(Objects::nonNull)
			.distinct()
			.toList());
	}

	private PartnerContactSetCommand requireContacts(PartnerContactSetCommand contacts) {
		if (contacts == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "파트너 연락처 정보는 필수입니다.");
		}
		return contacts;
	}

	private PartnerBusinessRegistrationCommand requireBusinessRegistration(PartnerBusinessRegistrationCommand businessRegistration) {
		if (businessRegistration == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록 정보는 필수입니다.");
		}
		return businessRegistration;
	}

	private PartnerBusinessRegistration toBusinessRegistration(PartnerBusinessRegistrationCommand command, String businessNumber) {
		return new PartnerBusinessRegistration(
			businessNumber,
			trim(command.companyName()),
			trim(command.ceoName()),
			trim(command.businessType()),
			trim(command.businessItem()),
			trimToNull(command.businessAddress()),
			trimToNull(command.businessAddressDetail()),
			trimToNull(command.settlementBankName()),
			trimToNull(command.settlementAccountNumber()),
			trimToNull(command.settlementAccountHolder()),
			trimToNull(command.taxInvoiceEmail()),
			command.issuedAt()
		);
	}

	private void applyBusinessRegistration(
		Partner partner,
		PartnerBusinessRegistrationCommand command,
		String businessNumber,
		Set<String> specifiedFields
	) {
		PartnerBusinessRegistration current = partner.businessRegistration();
		if (current == null) {
			partner.replaceBusinessRegistration(toBusinessRegistration(command, businessNumber));
			return;
		}
		current.update(
			businessNumber,
			specifiedFields.contains("company_name") ? trim(command.companyName()) : current.companyName(),
			specifiedFields.contains("ceo_name") ? trim(command.ceoName()) : current.ceoName(),
			specifiedFields.contains("business_type") ? trim(command.businessType()) : current.businessType(),
			specifiedFields.contains("business_item") ? trim(command.businessItem()) : current.businessItem(),
			specifiedFields.contains("business_address") ? trimToNull(command.businessAddress()) : current.businessAddress(),
			specifiedFields.contains("business_address_detail")
				? trimToNull(command.businessAddressDetail())
				: current.businessAddressDetail(),
			specifiedFields.contains("settlement_bank_name")
				? trimToNull(command.settlementBankName())
				: current.settlementBankName(),
			specifiedFields.contains("settlement_account_number")
				? trimToNull(command.settlementAccountNumber())
				: current.settlementAccountNumber(),
			specifiedFields.contains("settlement_account_holder")
				? trimToNull(command.settlementAccountHolder())
				: current.settlementAccountHolder(),
			specifiedFields.contains("tax_invoice_email")
				? trimToNull(command.taxInvoiceEmail())
				: current.taxInvoiceEmail(),
			specifiedFields.contains("issued_at") ? command.issuedAt() : current.issuedAt()
		);
	}

	private void assertBusinessNumberAvailableForUpdate(Partner partner, String businessNumber) {
		Optional<PartnerBusinessRegistration> existing = businessRegistrationRepository.findByBusinessNumber(businessNumber);
		if (existing.isPresent()
			&& (partner.businessRegistration() == null
				|| !Objects.equals(existing.get().id(), partner.businessRegistration().id()))) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}
	}

	private Set<PartnerFeature> loadFeatures(Set<Long> featureIds) {
		if (featureIds == null || featureIds.isEmpty()) {
			return Set.of();
		}
		List<PartnerFeature> features = featureRepository.findByIdInAndStatus(featureIds, PartnerFeatureStatus.ACTIVE);
		if (features.size() != featureIds.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 파트너 특징 정보가 올바르지 않습니다.");
		}
		return new LinkedHashSet<>(features);
	}

	private PartnerListItemResult toListItem(
		Partner partner,
		AccountPartner account,
		MediaResult logo,
		List<CategoryReferenceResult> categories,
		long specialistCount,
		long optionCount,
		PartnerAccountInvitation invitation
	) {
		return new PartnerListItemResult(
			partner.id(),
			partner.name(),
			partner.accountInvitationEmail(),
			partner.allowStatus().name(),
			partner.status().name(),
			partner.createdAt(),
			logo,
			accountResponse(account),
			assignedStaffResponse(partner.assignedStaff()),
			categories,
			partner.regionSortKey(),
			specialistCount,
			optionCount,
			partner.registrationSource().name(),
			accountLinkStatus(account, invitation).name(),
			invitation == null ? null : invitation.email(),
			invitation == null ? null : invitation.sentAt(),
			invitation == null ? null : invitation.expiresAt()
		);
	}

	private Map<Long, Long> resourceCounts(List<PartnerResourceCount> counts) {
		return counts.stream().collect(Collectors.toMap(
			PartnerResourceCount::getPartnerId,
			PartnerResourceCount::getItemCount
		));
	}

	private PartnerDetailResult toDetail(Partner partner) {
		return toDetail(partner, Set.of("business_registration"));
	}

	private PartnerDetailResult toDetail(
		Partner partner,
		Set<String> include
	) {
		PartnerAccountInvitation latestInvitation = latestInvitation(partner.id());
		return new PartnerDetailResult(
			partner.id(),
			partner.name(),
			partner.accountInvitationEmail(),
			partner.description(),
			partner.roadAddress(),
			partner.jibunAddress(),
			partner.latitude(),
			partner.longitude(),
			contacts(partner),
			contactResponses(partner.contacts()),
			partner.operatingHoursNotice(),
			fromJson(partner.operationHours()),
			partner.direction(),
			partner.viewCount(),
			0,
			partner.allowStatus().name(),
			partner.status().name(),
			latestStatusHistory(partner),
			partner.createdAt(),
			partner.updatedAt(),
			mediaReadService.primary(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_LOGO
			),
			mediaReadService.primary(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_MAIN_IMAGE
			),
			mediaReadService.list(
				MediaOwnerType.PARTNER,
				partner.id(),
				MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE
			),
			featureResponses(partner.features()),
			assignedStaffResponse(partner.assignedStaff()),
			include.contains("account_partner") ? accountResponse(partner.accountPartner()) : null,
			include.contains("specialists") ? specialistResponses(partner.id()) : null,
			include.contains("business_registration")
				? businessRegistrationResponse(partner.businessRegistration())
				: null,
			categoryAssignmentService.references(CategoryAssignmentTarget.PARTNER, partner.id()),
			partner.detailAddress(),
			hashtagRepository.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id())
				.stream().map(PartnerHashtag::value).toList(),
			linkRepository.findByPartner_IdOrderBySortOrderAscIdAsc(partner.id())
				.stream()
				.map(link -> new PartnerLinkResult(link.id(), link.type().name(), link.url(), link.sortOrder()))
				.toList(),
			optionService.listByPartnerId(partner.id()),
			partner.registrationSource().name(),
			partner.createdByStaffId(),
			accountLinkStatus(partner.accountPartner(), latestInvitation).name(),
			latestInvitation == null ? null : invitationResponse(latestInvitation)
		);
	}

	private List<PartnerSpecialistForStaffResult> specialistResponses(Long partnerId) {
		return specialistRepository.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId)
			.stream()
			.map(this::specialistResponse)
			.toList();
	}

	private PartnerSpecialistForStaffResult specialistResponse(Specialist specialist) {
		return new PartnerSpecialistForStaffResult(
			specialist.id(),
			specialist.partnerId(),
			specialist.name(),
			specialist.position(),
			new SpecialistFieldResult(
				specialist.specialistField().code(),
				specialist.specialistField().name(),
				specialist.specialistField().label()
			),
			specialist.sortOrder(),
			specialist.allowStatus().name(),
			specialist.status().name(),
			specialist.createdAt(),
			specialist.updatedAt()
		);
	}

	private OperationHistoryResult latestStatusHistory(Partner partner) {
		return operationHistoryRepository
			.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(OperationHistory.TARGET_PARTNER, partner.id())
			.stream()
			.filter(history -> history.changes().stream().anyMatch(change ->
				"status".equals(change.fieldKey()) && partner.status().name().equals(change.afterValue())))
			.findFirst()
			.map(this::operationHistoryResult)
			.orElse(null);
	}

	private OperationHistoryResult operationHistoryResult(OperationHistory history) {
		return new OperationHistoryResult(
			history.id(),
			history.action(),
			history.reason(),
			history.createdAt(),
			history.changes().stream()
				.map(change -> new OperationHistoryChangeResult(
					change.fieldKey(),
					change.beforeValue(),
					change.afterValue()
				))
				.toList()
		);
	}

	private PartnerAccountResult accountResponse(AccountPartner account) {
		if (account == null) {
			return null;
		}
		return new PartnerAccountResult(
			account.id(),
			account.name(),
			account.nickname(),
			account.email(),
			account.phone(),
			account.status().name(),
			account.lastLoginAt(),
			account.createdAt(),
			account.updatedAt()
		);
	}

	private PartnerAssignedStaffResult assignedStaffResponse(AccountStaff staff) {
		if (staff == null) {
			return null;
		}
		return new PartnerAssignedStaffResult(
			staff.id(),
			staff.name(),
			staff.nickname(),
			staff.email(),
			staff.status().name()
		);
	}

	private Long staffId(AccountStaff staff) {
		return staff == null ? null : staff.id();
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private PartnerBusinessRegistrationResult businessRegistrationResponse(PartnerBusinessRegistration registration) {
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
			new PartnerSettlementAccountResult(
				registration.settlementBankName(),
				registration.settlementAccountNumber(),
				registration.settlementAccountHolder(),
				registration.taxInvoiceEmail()
			),
			registration.issuedAt(),
			registration.status().name(),
			mediaReadService.primary(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
			)
		);
	}

	private PartnerContactGroupResult contacts(Partner partner) {
		Map<PartnerContactType, List<String>> byType = partner.contacts().stream()
			.filter(PartnerContact::active)
			.sorted(Comparator.comparing(PartnerContact::sortOrder).thenComparing(PartnerContact::id))
			.collect(Collectors.groupingBy(
				PartnerContact::contactType,
				LinkedHashMap::new,
				Collectors.mapping(PartnerContact::value, Collectors.toList())
			));
		return new PartnerContactGroupResult(
			first(byType.get(PartnerContactType.REPRESENTATIVE_PHONE)),
			first(byType.get(PartnerContactType.SMS_SENDER_PHONE)),
			first(byType.get(PartnerContactType.CALL_RECEIVER_PHONE)),
			byType.getOrDefault(PartnerContactType.CONSULTATION_RECEIVER_PHONE, List.of()),
			byType.getOrDefault(PartnerContactType.EVENT_NOTICE_RECEIVER_PHONE, List.of()),
			byType.getOrDefault(PartnerContactType.NOTICE_MARKETING_EMAIL, List.of())
		);
	}

	private List<PartnerContactResult> contactResponses(Set<PartnerContact> contacts) {
		return contacts.stream()
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

	private List<PartnerFeatureResult> featureResponses(Set<PartnerFeature> features) {
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

	private Map<Long, AccountPartner> accountsByPartnerIds(List<Partner> partners) {
		List<Long> partnerIds = partners.stream().map(Partner::id).toList();
		if (partnerIds.isEmpty()) {
			return Map.of();
		}
		return accountPartnerRepository.findByPartner_IdInAndDeletedAtIsNull(partnerIds)
			.stream()
			.collect(Collectors.toMap(AccountPartner::partnerId, account -> account));
	}

	private Map<Long, PartnerAccountInvitation> latestInvitationsByPartnerIds(List<Partner> partners) {
		List<Long> partnerIds = partners.stream().map(Partner::id).toList();
		if (partnerIds.isEmpty()) {
			return Map.of();
		}
		return invitationRepository.findByPartner_IdInOrderByCreatedAtDescIdDesc(partnerIds)
			.stream()
			.collect(Collectors.toMap(
				PartnerAccountInvitation::partnerId,
				invitation -> invitation,
				(first, ignored) -> first,
				LinkedHashMap::new
			));
	}

	private PartnerAccountInvitation latestInvitation(Long partnerId) {
		return invitationRepository.findByPartner_IdOrderByCreatedAtDescIdDesc(partnerId)
			.stream()
			.findFirst()
			.orElse(null);
	}

	private PartnerAccountLinkStatus accountLinkStatus(
		AccountPartner account,
		PartnerAccountInvitation invitation
	) {
		if (account != null) {
			return PartnerAccountLinkStatus.CONNECTED;
		}
		if (invitation == null) {
			return PartnerAccountLinkStatus.NOT_INVITED;
		}
		return switch (invitation.status()) {
			case PENDING -> PartnerAccountLinkStatus.INVITED;
			case ACCEPTED -> PartnerAccountLinkStatus.CONNECTED;
			case CANCELED -> PartnerAccountLinkStatus.NOT_INVITED;
		};
	}

	private PartnerAccountInvitationResult invitationResponse(PartnerAccountInvitation invitation) {
		return new PartnerAccountInvitationResult(
			invitation.id(),
			invitation.partnerId(),
			invitation.email(),
			invitation.status().name(),
			invitation.expiresAt(),
			invitation.sentAt(),
			invitation.acceptedAt(),
			invitation.canceledAt(),
			invitation.createdByStaffId(),
			invitation.createdAt(),
			invitation.updatedAt()
		);
	}

	private Map<String, String> capture(Partner partner) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("description", partner.description());
		values.put("account_invitation_email", partner.accountInvitationEmail());
		values.put("categories", writeInternalJson(
			categoryAssignmentService.references(CategoryAssignmentTarget.PARTNER, partner.id())
		));
		values.put("road_address", partner.roadAddress());
		values.put("jibun_address", partner.jibunAddress());
		values.put("detail_address", partner.detailAddress());
		values.put("latitude", partner.latitude());
		values.put("longitude", partner.longitude());
		values.put("operating_hours_notice", partner.operatingHoursNotice());
		values.put("operation_hours", partner.operationHours());
		values.put("direction", partner.direction());
		values.put("allow_status", partner.allowStatus().name());
		values.put("status", partner.status().name());
		values.put("contacts", writeInternalJson(contacts(partner)));
		PartnerBusinessRegistration registration = partner.businessRegistration();
		if (registration != null) {
			Map<String, Object> business = new LinkedHashMap<>();
			business.put("business_number", registration.businessNumber());
			business.put("company_name", registration.companyName());
			business.put("ceo_name", registration.ceoName());
			business.put("business_type", registration.businessType());
			business.put("business_item", registration.businessItem());
			business.put("business_address", registration.businessAddress());
			business.put("business_address_detail", registration.businessAddressDetail());
			business.put("settlement_bank_name", registration.settlementBankName());
			business.put("settlement_account_number", registration.settlementAccountNumber());
			business.put("settlement_account_holder", registration.settlementAccountHolder());
			business.put("tax_invoice_email", registration.taxInvoiceEmail());
			business.put("issued_at", registration.issuedAt());
			values.put("business_registration", writeInternalJson(business));
			values.put("business_registration_file", writeInternalJson(mediaSnapshot(mediaReadService.primary(
				MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE
			))));
		}
		values.put("features", writeInternalJson(featureResponses(partner.features())));
		values.put("logo", writeInternalJson(mediaSnapshot(mediaReadService.primary(
			MediaOwnerType.PARTNER,
			partner.id(),
			MediaCollectionPolicy.PARTNER_LOGO
		))));
		values.put("main_image", writeInternalJson(mediaSnapshot(mediaReadService.primary(
			MediaOwnerType.PARTNER,
			partner.id(),
			MediaCollectionPolicy.PARTNER_MAIN_IMAGE
		))));
		values.put("interior_images", writeInternalJson(mediaReadService.list(
			MediaOwnerType.PARTNER,
			partner.id(),
			MediaCollectionPolicy.PARTNER_INTERIOR_IMAGE
		).stream().map(this::mediaSnapshot).toList()));
		return values;
	}

	private Map<String, Object> mediaSnapshot(MediaResult media) {
		if (media == null) {
			return null;
		}
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("id", media.id());
		value.put("name", media.originalName());
		value.put("mime_type", media.mimeType());
		value.put("size", media.size());
		return value;
	}

	private String writeInternalJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("파트너 변경 이력 JSON을 만들 수 없습니다.", exception);
		}
	}

	private void recordSimpleHistory(
		AuthenticatedActor actor,
		Partner partner,
		String action,
		String reason
	) {
		operationHistoryRepository.save(new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			trimToNull(reason),
			null
		));
	}

	private void recordChangedHistory(
		AuthenticatedActor actor,
		Partner partner,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			trimToNull(reason),
			null
		);
		before.forEach((key, beforeValue) -> {
			String afterValue = after.get(key);
			if (!Objects.equals(beforeValue, afterValue)) {
				history.addChange(key, beforeValue, afterValue);
			}
		});
		if (!history.changes().isEmpty()) {
			operationHistoryRepository.save(history);
		}
	}

	private String normalizeOperationHours(Object value) {
		if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
			return null;
		}
		try {
			JsonNode root = value instanceof String rawValue
				? objectMapper.readTree(rawValue)
				: objectMapper.valueToTree(value);
			if (root == null || !root.isObject()) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "운영시간 형식이 올바르지 않습니다.");
			}
			for (String day : List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun")) {
				JsonNode hours = root.get(day);
				if (hours == null || !hours.isObject() || !hours.path("is_closed").isBoolean()) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "요일별 운영시간을 모두 입력해주세요.");
				}
				if (hours.path("is_closed").booleanValue()) {
					continue;
				}
				String start = hours.path("start").isTextual() ? hours.path("start").textValue() : null;
				String end = hours.path("end").isTextual() ? hours.path("end").textValue() : null;
				if (start == null || end == null || !start.matches("^\\d{2}:\\d{2}$") || !end.matches("^\\d{2}:\\d{2}$")) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "운영 시작 시간과 종료 시간을 HH:mm 형식으로 입력해주세요.");
				}
				LocalTime startTime = LocalTime.parse(start);
				LocalTime endTime = LocalTime.parse(end);
				if (!endTime.isAfter(startTime)) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "운영 종료 시간은 시작 시간보다 늦어야 합니다.");
				}
			}
			return objectMapper.writeValueAsString(root);
		} catch (ApiException exception) {
			throw exception;
		} catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "운영시간 형식이 올바르지 않습니다.");
		}
	}

	private Object fromJson(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 파트너 운영시간 JSON이 올바르지 않습니다.", exception);
		}
	}

	private void revokePartnerAccountSessionsWhenWithdrawn(
		Partner partner,
		PartnerStatus previousStatus
	) {
		if (previousStatus != PartnerStatus.WITHDRAWN && partner.status() == PartnerStatus.WITHDRAWN) {
			revokePartnerAccountSessions(partner, "PARTNER_WITHDRAWN");
		}
	}

	private void assertPartnerStatusTransition(PartnerStatus before, PartnerStatus after) {
		if (before == PartnerStatus.WITHDRAWN && after != PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "탈퇴한 파트너은 운영상태를 변경할 수 없습니다.");
		}
	}

	private void assertPartnerAllowStatusTransition(
		PartnerAllowStatus before,
		PartnerAllowStatus after
	) {
		if (before == after) {
			return;
		}
		boolean reviewDecision = before == PartnerAllowStatus.PENDING
			&& (after == PartnerAllowStatus.APPROVED || after == PartnerAllowStatus.REJECTED);
		if (!reviewDecision) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"Staff can review only PENDING partners as APPROVED or REJECTED."
			);
		}
	}

	private void requireRejectionReason(PartnerAllowStatus status, String reason) {
		if (status == PartnerAllowStatus.REJECTED && trimToNull(reason) == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A rejection reason is required.");
		}
	}

	private void revokePartnerAccountSessions(Partner partner, String reason) {
		AccountPartner account = partner.accountPartner();
		if (account != null) {
			authSessionService.revokeAll(AccountActorType.PARTNER, account.id(), reason);
		}
	}

	private String normalizeBusinessNumber(String value) {
		String trimmed = trim(value);
		String normalized = trimmed.replaceAll("\\D+", "");
		return normalized.isBlank() ? trimmed : normalized;
	}

	private String normalizeEmail(String value) {
		String trimmed = trimToNull(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 yyyy-MM-dd 이어야 합니다.");
		}
	}

	private Optional<Long> parseLong(String value) {
		String normalized = value.replaceAll("(?i)^(HID|UID)[-_ ]?", "");
		if (!normalized.matches("\\d+")) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(normalized));
	}

	private String trim(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 값이 누락되었습니다.");
		}
		return trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String first(List<String> values) {
		return values == null || values.isEmpty() ? null : values.getFirst();
	}

	private int clamp(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}
}
