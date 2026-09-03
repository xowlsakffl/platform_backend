package com.platform.application.partner;

import com.platform.application.auth.PermissionService;
import com.platform.application.cache.StaffSummaryCache;
import com.platform.application.cache.StaffSummaryCacheInvalidator;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.partner.command.CreateOwnedPartnerCommand;
import com.platform.application.partner.result.OwnedPartnerResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.account.AccountPartner;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.media.MediaOwnerType;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerBusinessRegistration;
import com.platform.domain.partner.PartnerContact;
import com.platform.domain.partner.PartnerContactType;
import com.platform.domain.partner.PartnerMembership;
import com.platform.domain.partner.PartnerMembershipStatus;
import com.platform.infrastructure.persistence.account.AccountPartnerRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import com.platform.infrastructure.persistence.partner.PartnerBusinessRegistrationRepository;
import com.platform.infrastructure.persistence.partner.PartnerMembershipRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerWorkspaceService {

	private static final String ACTION_PARTNER_CREATED = "PARTNER_REGISTRATION_SUBMITTED";
	private final PermissionService permissionService;
	private final CategoryAssignmentService categoryAssignmentService;
	private final PartnerBusinessNumberPolicy businessNumberPolicy;
	private final MediaCommandService mediaCommandService;
	private final AccountPartnerRepository accountRepository;
	private final PartnerRepository partnerRepository;
	private final PartnerBusinessRegistrationRepository businessRegistrationRepository;
	private final PartnerMembershipRepository membershipRepository;
	private final OperationHistoryRepository historyRepository;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;

	public PartnerWorkspaceService(
		PermissionService permissionService,
		CategoryAssignmentService categoryAssignmentService,
		PartnerBusinessNumberPolicy businessNumberPolicy,
		MediaCommandService mediaCommandService,
		AccountPartnerRepository accountRepository,
		PartnerRepository partnerRepository,
		PartnerBusinessRegistrationRepository businessRegistrationRepository,
		PartnerMembershipRepository membershipRepository,
		OperationHistoryRepository historyRepository,
		StaffSummaryCacheInvalidator summaryCacheInvalidator
	) {
		this.permissionService = permissionService;
		this.categoryAssignmentService = categoryAssignmentService;
		this.businessNumberPolicy = businessNumberPolicy;
		this.mediaCommandService = mediaCommandService;
		this.accountRepository = accountRepository;
		this.partnerRepository = partnerRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.membershipRepository = membershipRepository;
		this.historyRepository = historyRepository;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
	}

	@Transactional(readOnly = true)
	public List<OwnedPartnerResult> list(AuthenticatedActor actor) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		return membershipRepository.findAllForAccount(actor.accountId(), PartnerMembershipStatus.ACTIVE)
			.stream()
			.map(this::result)
			.toList();
	}

	@Transactional
	public OwnedPartnerResult create(AuthenticatedActor actor, CreateOwnedPartnerCommand command) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		String name = requireName(command.name());
		String businessNumber = businessNumberPolicy.normalize(command.businessNumber());
		if (businessRegistrationRepository.existsByBusinessNumber(businessNumber)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}
		AccountPartner account = accountRepository.findForUpdateByIdAndDeletedAtIsNull(actor.accountId())
			.filter(AccountPartner::isActive)
			.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
		String roadAddress = requireText(command.roadAddress(), "주소를 입력해주세요.");
		String detailAddress = trimToNull(command.detailAddress());

		Partner partner = Partner.createDraft(name);
		partner.updateOnboardingProfile(
			name,
			trimToNull(command.englishName()),
			null,
			roadAddress,
			detailAddress,
			trimToNull(command.latitude()),
			trimToNull(command.longitude()),
			null,
			null,
			null
		);
		partner.replaceContacts(registrationContacts(command));
		partner.replaceBusinessRegistration(new PartnerBusinessRegistration(
			businessNumber,
			requireText(command.companyName(), "상호를 입력해주세요."),
			requireText(command.ceoName(), "대표자명을 입력해주세요."),
			command.openingDate(),
			null,
			null,
			null
		));
		try {
			partner = partnerRepository.saveAndFlush(partner);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}
		PartnerMembership membership = membershipRepository.saveAndFlush(
			PartnerMembership.owner(account, partner)
		);
		categoryAssignmentService.replacePrimary(
			CategoryAssignmentTarget.PARTNER,
			partner.id(),
			command.categoryId()
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.PARTNER_BUSINESS_REGISTRATION,
			partner.businessRegistration().id(),
			MediaCollectionPolicy.PARTNER_BUSINESS_REGISTRATION_FILE,
			command.businessRegistrationFile(),
			null,
			true
		);
		partner.requestReview();
		partnerRepository.saveAndFlush(partner);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			partner.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_PARTNER_CREATED,
			null,
			null
		).captureActor(actor.name(), actor.loginId());
		history.addChange(
			"allow_status",
			PartnerAllowStatus.DRAFT.name(),
			PartnerAllowStatus.REVIEW_REQUESTED.name()
		);
		historyRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.PARTNER);
		return result(membership);
	}

	private Set<PartnerContact> registrationContacts(CreateOwnedPartnerCommand command) {
		Set<PartnerContact> contacts = new LinkedHashSet<>();
		contacts.add(new PartnerContact(
			PartnerContactType.REPRESENTATIVE_PHONE,
			requireText(command.representativePhone(), "대표 전화번호를 입력해주세요."),
			0,
			true
		));
		contacts.add(new PartnerContact(
			PartnerContactType.REPRESENTATIVE_EMAIL,
			requireText(command.representativeEmail(), "대표 이메일을 입력해주세요.").toLowerCase(Locale.ROOT),
			0,
			true
		));
		return contacts;
	}

	private OwnedPartnerResult result(PartnerMembership membership) {
		Partner partner = membership.partner();
		return new OwnedPartnerResult(
			partner.id(),
			partner.name(),
			partner.allowStatus().name(),
			partner.status().name(),
			membership.role().name(),
			partner.createdAt()
		);
	}

	private String requireName(String value) {
		if (!StringUtils.hasText(value)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표 업체명을 입력해주세요.");
		}
		String name = value.trim();
		if (name.length() > 30) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표 업체명은 30자 이하로 입력해주세요.");
		}
		return name;
	}

	private String requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return value.trim();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
