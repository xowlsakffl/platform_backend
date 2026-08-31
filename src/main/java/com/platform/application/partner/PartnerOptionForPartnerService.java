package com.platform.application.partner;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.auth.PermissionService;
import com.platform.application.category.CategoryAssignmentService;
import com.platform.application.category.result.CategoryReferenceResult;
import com.platform.application.partner.command.SavePartnerOptionCommand;
import com.platform.application.partner.command.ReplacePartnerOptionsCommand;
import com.platform.application.partner.command.SavePartnerOptionCommand.SpecialistPriceCommand;
import com.platform.application.partner.result.PartnerOptionResult;
import com.platform.application.partner.result.PartnerOptionResult.SpecialistPriceResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.AccessPermissions;
import com.platform.domain.partner.Partner;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerOption;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistOption;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerOptionForPartnerService {

	private final OwnershipPolicy ownershipPolicy;
	private final PermissionService permissionService;
	private final CategoryAssignmentService categoryAssignmentService;
	private final PartnerRepository partnerRepository;
	private final PartnerOptionRepository optionRepository;
	private final SpecialistRepository specialistRepository;
	private final SpecialistOptionRepository specialistOptionRepository;

	public PartnerOptionForPartnerService(
		OwnershipPolicy ownershipPolicy,
		PermissionService permissionService,
		CategoryAssignmentService categoryAssignmentService,
		PartnerRepository partnerRepository,
		PartnerOptionRepository optionRepository,
		SpecialistRepository specialistRepository,
		SpecialistOptionRepository specialistOptionRepository
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.permissionService = permissionService;
		this.categoryAssignmentService = categoryAssignmentService;
		this.partnerRepository = partnerRepository;
		this.optionRepository = optionRepository;
		this.specialistRepository = specialistRepository;
		this.specialistOptionRepository = specialistOptionRepository;
	}

	@Transactional(readOnly = true)
	public List<PartnerOptionResult> list(AuthenticatedActor actor) {
		Partner partner = ownedPartner(actor);
		return listByPartnerId(partner.id());
	}

	@Transactional(readOnly = true)
	public List<PartnerOptionResult> listByPartnerId(Long partnerId) {
		return results(optionRepository
			.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId));
	}

	@Transactional(readOnly = true)
	public List<PartnerOptionResult> listForStaff(AuthenticatedActor actor, Long partnerId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		staffPartner(partnerId, false);
		return listByPartnerId(partnerId);
	}

	@Transactional(readOnly = true)
	public void validatePartnerCategoryChange(Long partnerId, Long categoryId) {
		categoryAssignmentService.requireSelectable(CategoryAssignmentTarget.PARTNER, categoryId);
		List<PartnerOption> options = optionRepository
			.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId);
		if (options.isEmpty()) {
			return;
		}
		Map<Long, List<CategoryReferenceResult>> categoriesByOptionId = categoryAssignmentService
			.referencesByTargetIds(CategoryAssignmentTarget.PARTNER_OPTION, options.stream().map(PartnerOption::id).toList());
		boolean incompatible = options.stream().anyMatch(option -> {
			List<CategoryReferenceResult> categories = categoriesByOptionId.getOrDefault(option.id(), List.of());
			return categories.size() != 1 || !Objects.equals(categories.getFirst().parentId(), categoryId);
		});
		if (incompatible) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"Partner category cannot be changed while options belong to another category."
			);
		}
	}

	@Transactional
	public PartnerOptionResult create(AuthenticatedActor actor, SavePartnerOptionCommand command) {
		Partner partner = editablePartner(actor);
		return createForPartner(partner, command);
	}

	PartnerOptionResult createForPartner(Partner partner, SavePartnerOptionCommand command) {
		ValidatedOption value = validate(partner, command);
		PartnerOption option = optionRepository.saveAndFlush(new PartnerOption(
			partner,
			value.name(),
			value.description(),
			value.regularPrice(),
			value.salePrice(),
			value.durationMinutes(),
			value.visible(),
			value.sortOrder()
		));
		categoryAssignmentService.replacePrimary(
			CategoryAssignmentTarget.PARTNER_OPTION,
			option.id(),
			value.categoryId()
		);
		replaceSpecialists(partner, option, value.specialists());
		return result(option, specialistOptionRepository
			.findByPartnerOption_IdAndSpecialist_DeletedAtIsNullOrderBySpecialist_SortOrderAscSpecialist_IdAsc(option.id()), optionCategory(option.id()));
	}

	@Transactional
	public PartnerOptionResult createForStaff(
		AuthenticatedActor actor,
		Long partnerId,
		SavePartnerOptionCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		return createForPartner(staffPartner(partnerId, true), command);
	}

	@Transactional
	public List<PartnerOptionResult> replaceForStaff(
		AuthenticatedActor actor,
		Long partnerId,
		ReplacePartnerOptionsCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		Partner partner = staffPartnerForUpdate(partnerId);
		List<PartnerOption> existing = optionRepository
			.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId);
		Map<Long, PartnerOption> existingById = existing.stream()
			.collect(Collectors.toMap(PartnerOption::id, option -> option));
		List<ReplacePartnerOptionsCommand.Item> requested = command.options() == null
			? List.of()
			: command.options();
		Set<Long> requestedIds = new HashSet<>();

		for (ReplacePartnerOptionsCommand.Item item : requested) {
			if (item.id() != null
				&& (!requestedIds.add(item.id()) || !existingById.containsKey(item.id()))) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "Partner option id is invalid or duplicated.");
			}
			validate(partner, item.value());
		}

		for (ReplacePartnerOptionsCommand.Item item : requested) {
			if (item.id() == null) {
				createForPartner(partner, item.value());
			} else {
				updateForPartner(partner, item.id(), item.value());
			}
		}
		for (PartnerOption option : existing) {
			if (!requestedIds.contains(option.id())) {
				deleteForPartner(partner, option.id());
			}
		}
		return listByPartnerId(partnerId);
	}

	@Transactional
	public PartnerOptionResult update(
		AuthenticatedActor actor,
		Long optionId,
		SavePartnerOptionCommand command
	) {
		Partner partner = editablePartner(actor);
		return updateForPartner(partner, optionId, command);
	}

	private PartnerOptionResult updateForPartner(
		Partner partner,
		Long optionId,
		SavePartnerOptionCommand command
	) {
		PartnerOption option = optionRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(optionId, partner.id())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner option not found."));
		ValidatedOption value = validate(partner, command);
		option.update(
			value.name(),
			value.description(),
			value.regularPrice(),
			value.salePrice(),
			value.durationMinutes(),
			value.visible(),
			value.sortOrder()
		);
		optionRepository.saveAndFlush(option);
		categoryAssignmentService.replacePrimary(
			CategoryAssignmentTarget.PARTNER_OPTION,
			option.id(),
			value.categoryId()
		);
		replaceSpecialists(partner, option, value.specialists());
		return result(option, specialistOptionRepository
			.findByPartnerOption_IdAndSpecialist_DeletedAtIsNullOrderBySpecialist_SortOrderAscSpecialist_IdAsc(option.id()), optionCategory(option.id()));
	}

	@Transactional
	public PartnerOptionResult updateForStaff(
		AuthenticatedActor actor,
		Long partnerId,
		Long optionId,
		SavePartnerOptionCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		return updateForPartner(staffPartner(partnerId, true), optionId, command);
	}

	@Transactional
	public Long delete(AuthenticatedActor actor, Long optionId) {
		Partner partner = editablePartner(actor);
		return deleteForPartner(partner, optionId);
	}

	private Long deleteForPartner(Partner partner, Long optionId) {
		PartnerOption option = optionRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(optionId, partner.id())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner option not found."));
		specialistOptionRepository.deleteByPartnerOption_Id(option.id());
		categoryAssignmentService.deleteAll(CategoryAssignmentTarget.PARTNER_OPTION, option.id());
		option.softDelete();
		optionRepository.save(option);
		return option.id();
	}

	@Transactional
	public Long deleteForStaff(AuthenticatedActor actor, Long partnerId, Long optionId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_UPDATE);
		return deleteForPartner(staffPartner(partnerId, true), optionId);
	}

	private Partner ownedPartner(AuthenticatedActor actor) {
		ownershipPolicy.requirePartnerOwner(actor, actor.partnerId());
		return partnerRepository.findByIdAndDeletedAtIsNull(actor.partnerId())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
	}

	private Partner editablePartner(AuthenticatedActor actor) {
		Partner partner = ownedPartner(actor);
		if (partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot edit options.");
		}
		if (partner.allowStatus() == PartnerAllowStatus.REVIEW_REQUESTED
			|| partner.allowStatus() == PartnerAllowStatus.IN_REVIEW) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Options cannot be changed while review is pending.");
		}
		return partner;
	}

	private Partner staffPartner(Long partnerId, boolean editable) {
		Partner partner = partnerRepository.findByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
		if (editable && partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot edit options.");
		}
		return partner;
	}

	private Partner staffPartnerForUpdate(Long partnerId) {
		Partner partner = partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner not found."));
		if (partner.status() == PartnerStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A withdrawn partner cannot edit options.");
		}
		return partner;
	}

	private ValidatedOption validate(Partner partner, SavePartnerOptionCommand command) {
		Category category = categoryAssignmentService.requireSelectable(
			CategoryAssignmentTarget.PARTNER_OPTION,
			command.categoryId()
		);
		if (category.parentId() == null || !categoryAssignmentService.isAssigned(
			CategoryAssignmentTarget.PARTNER,
			partner.id(),
			category.parentId()
		)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Option category must belong to the partner category.");
		}
		String name = trimToNull(command.name());
		if (name == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Option name is required.");
		}
		validatePricePair(command.regularPrice(), command.salePrice(), "Option");
		List<SpecialistPriceCommand> specialists = command.specialists() == null ? List.of() : command.specialists();
		long uniqueCount = specialists.stream().map(SpecialistPriceCommand::specialistId).distinct().count();
		if (uniqueCount != specialists.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A specialist can be assigned only once per option.");
		}
		for (SpecialistPriceCommand specialist : specialists) {
			if (specialist.specialistId() == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "Specialist id is required.");
			}
			if (specialist.regularPriceOverride() == null && specialist.salePriceOverride() != null) {
				throw new ApiException(
					ErrorCode.INVALID_REQUEST,
					"Specialist regular price is required when a sale price is provided."
				);
			}
			if (specialist.regularPriceOverride() != null) {
				validatePricePair(
					specialist.regularPriceOverride(),
					specialist.salePriceOverride(),
					"Specialist"
				);
			}
		}
		return new ValidatedOption(
			category.id(),
			name,
			trimToNull(command.description()),
			command.regularPrice(),
			command.salePrice(),
			command.durationMinutes(),
			command.visible(),
			command.sortOrder(),
			specialists
		);
	}

	private void validatePricePair(BigDecimal regularPrice, BigDecimal salePrice, String fieldName) {
		if (regularPrice == null || regularPrice.signum() < 0) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				fieldName + " regular price is required and cannot be negative."
			);
		}
		if (salePrice == null) {
			return;
		}
		if (salePrice.signum() < 0 || salePrice.compareTo(regularPrice) >= 0) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				fieldName + " sale price must be lower than the regular price."
			);
		}
	}

	private void replaceSpecialists(
		Partner partner,
		PartnerOption option,
		List<SpecialistPriceCommand> commands
	) {
		specialistOptionRepository.deleteByPartnerOption_Id(option.id());
		if (commands.isEmpty()) {
			return;
		}
		List<Long> specialistIds = commands.stream().map(SpecialistPriceCommand::specialistId).toList();
		Map<Long, Specialist> specialists = specialistRepository
			.findAllById(specialistIds)
			.stream()
			.filter(specialist -> specialist.deletedAt() == null)
			.collect(Collectors.toMap(Specialist::id, specialist -> specialist));
		if (specialists.size() != specialistIds.size()
			|| specialists.values().stream().anyMatch(specialist -> !partner.id().equals(specialist.partnerId()))) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A specialist does not belong to this partner.");
		}
		List<SpecialistOption> assignments = commands.stream()
			.map(command -> new SpecialistOption(
				specialists.get(command.specialistId()),
					option,
					command.regularPriceOverride(),
					command.salePriceOverride()
			))
			.toList();
		specialistOptionRepository.saveAll(assignments);
	}

	private List<PartnerOptionResult> results(List<PartnerOption> options) {
		if (options.isEmpty()) {
			return List.of();
		}
		Map<Long, List<SpecialistOption>> byOptionId = specialistOptionRepository
				.findByPartnerOption_IdInAndSpecialist_DeletedAtIsNull(options.stream().map(PartnerOption::id).toList())
			.stream()
			.collect(Collectors.groupingBy(
				assignment -> assignment.partnerOption().id(),
				LinkedHashMap::new,
				Collectors.toList()
			));
		Map<Long, List<CategoryReferenceResult>> categoriesByOptionId = categoryAssignmentService
			.referencesByTargetIds(CategoryAssignmentTarget.PARTNER_OPTION, options.stream().map(PartnerOption::id).toList());
		return options.stream()
			.map(option -> result(
				option,
				byOptionId.getOrDefault(option.id(), List.of()),
				categoriesByOptionId.getOrDefault(option.id(), List.of()).stream().findFirst().orElse(null)
			))
			.toList();
	}

	private PartnerOptionResult result(
		PartnerOption option,
		List<SpecialistOption> assignments,
		CategoryReferenceResult category
	) {
		return new PartnerOptionResult(
			option.id(),
			category,
			option.name(),
			option.description(),
			option.regularPrice(),
			option.salePrice(),
			option.effectivePrice(),
			option.discountRate(),
			option.durationMinutes(),
			option.visible(),
			option.sortOrder(),
			assignments.stream()
				.map(assignment -> new SpecialistPriceResult(
					assignment.specialist().id(),
					assignment.specialist().name(),
						assignment.regularPriceOverride(),
						assignment.salePriceOverride(),
						assignment.effectiveRegularPrice(),
					assignment.effectiveSalePrice(),
					assignment.effectivePrice(),
						assignment.effectiveDiscountRate()
				))
				.toList(),
			option.createdAt(),
			option.updatedAt()
		);
	}

	private CategoryReferenceResult optionCategory(Long optionId) {
		return categoryAssignmentService.references(CategoryAssignmentTarget.PARTNER_OPTION, optionId)
			.stream()
			.findFirst()
			.orElse(null);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record ValidatedOption(
		Long categoryId,
		String name,
		String description,
		BigDecimal regularPrice,
		BigDecimal salePrice,
		Integer durationMinutes,
		boolean visible,
		int sortOrder,
		List<SpecialistPriceCommand> specialists
	) {
	}
}
