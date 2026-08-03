package com.platform.application.partner;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.partner.command.SavePartnerOptionCommand;
import com.platform.application.partner.command.SavePartnerOptionCommand.SpecialistPriceCommand;
import com.platform.application.partner.result.PartnerOptionResult;
import com.platform.application.partner.result.PartnerOptionResult.SpecialistPriceResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerAllowStatus;
import com.platform.domain.partner.PartnerOption;
import com.platform.domain.partner.PartnerPriceType;
import com.platform.domain.partner.PartnerStatus;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistOption;
import com.platform.infrastructure.persistence.partner.PartnerOptionRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistOptionRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerOptionForPartnerService {

	private final OwnershipPolicy ownershipPolicy;
	private final PartnerRepository partnerRepository;
	private final PartnerOptionRepository optionRepository;
	private final SpecialistRepository specialistRepository;
	private final SpecialistOptionRepository specialistOptionRepository;

	public PartnerOptionForPartnerService(
		OwnershipPolicy ownershipPolicy,
		PartnerRepository partnerRepository,
		PartnerOptionRepository optionRepository,
		SpecialistRepository specialistRepository,
		SpecialistOptionRepository specialistOptionRepository
	) {
		this.ownershipPolicy = ownershipPolicy;
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

	@Transactional
	public PartnerOptionResult create(AuthenticatedActor actor, SavePartnerOptionCommand command) {
		Partner partner = editablePartner(actor);
		ValidatedOption value = validate(command);
		PartnerOption option = optionRepository.saveAndFlush(new PartnerOption(
			partner,
			value.name(),
			value.description(),
			value.price(),
			value.priceType(),
			value.durationMinutes(),
			value.visible(),
			value.sortOrder()
		));
		replaceSpecialists(partner, option, value.specialists());
		return result(option, specialistOptionRepository
			.findByPartnerOption_IdOrderBySpecialist_SortOrderAscSpecialist_IdAsc(option.id()));
	}

	@Transactional
	public PartnerOptionResult update(
		AuthenticatedActor actor,
		Long optionId,
		SavePartnerOptionCommand command
	) {
		Partner partner = editablePartner(actor);
		PartnerOption option = optionRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(optionId, partner.id())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner option not found."));
		ValidatedOption value = validate(command);
		option.update(
			value.name(),
			value.description(),
			value.price(),
			value.priceType(),
			value.durationMinutes(),
			value.visible(),
			value.sortOrder()
		);
		optionRepository.saveAndFlush(option);
		replaceSpecialists(partner, option, value.specialists());
		return result(option, specialistOptionRepository
			.findByPartnerOption_IdOrderBySpecialist_SortOrderAscSpecialist_IdAsc(option.id()));
	}

	@Transactional
	public Long delete(AuthenticatedActor actor, Long optionId) {
		Partner partner = editablePartner(actor);
		PartnerOption option = optionRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNull(optionId, partner.id())
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Partner option not found."));
		specialistOptionRepository.deleteByPartnerOption_Id(option.id());
		option.softDelete();
		optionRepository.save(option);
		return option.id();
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
		if (partner.allowStatus() == PartnerAllowStatus.PENDING) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Options cannot be changed while review is pending.");
		}
		return partner;
	}

	private ValidatedOption validate(SavePartnerOptionCommand command) {
		String name = trimToNull(command.name());
		if (name == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Option name is required.");
		}
		PartnerPriceType priceType = Objects.requireNonNull(command.priceType(), "priceType");
		validatePrice(priceType, command.price(), "Option price");
		List<SpecialistPriceCommand> specialists = command.specialists() == null ? List.of() : command.specialists();
		long uniqueCount = specialists.stream().map(SpecialistPriceCommand::specialistId).distinct().count();
		if (uniqueCount != specialists.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "A specialist can be assigned only once per option.");
		}
		for (SpecialistPriceCommand specialist : specialists) {
			if (specialist.specialistId() == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "Specialist id is required.");
			}
			PartnerPriceType effectiveType = specialist.priceTypeOverride() == null
				? priceType
				: specialist.priceTypeOverride();
			if (specialist.priceOverride() != null || specialist.priceTypeOverride() != null) {
				validatePrice(effectiveType, specialist.priceOverride(), "Specialist price");
			}
		}
		return new ValidatedOption(
			name,
			trimToNull(command.description()),
			priceType == PartnerPriceType.INQUIRE ? null : command.price(),
			priceType,
			command.durationMinutes(),
			command.visible(),
			command.sortOrder(),
			specialists
		);
	}

	private void validatePrice(PartnerPriceType type, BigDecimal price, String fieldName) {
		if (type == PartnerPriceType.INQUIRE) {
			if (price != null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " must be empty for INQUIRE.");
			}
			return;
		}
		if (price == null || price.signum() < 0) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " is required and cannot be negative.");
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
				command.priceOverride(),
				command.priceTypeOverride()
			))
			.toList();
		specialistOptionRepository.saveAll(assignments);
	}

	private List<PartnerOptionResult> results(List<PartnerOption> options) {
		if (options.isEmpty()) {
			return List.of();
		}
		Map<Long, List<SpecialistOption>> byOptionId = specialistOptionRepository
			.findByPartnerOption_IdIn(options.stream().map(PartnerOption::id).toList())
			.stream()
			.collect(Collectors.groupingBy(
				assignment -> assignment.partnerOption().id(),
				LinkedHashMap::new,
				Collectors.toList()
			));
		return options.stream()
			.map(option -> result(option, byOptionId.getOrDefault(option.id(), List.of())))
			.toList();
	}

	private PartnerOptionResult result(PartnerOption option, List<SpecialistOption> assignments) {
		return new PartnerOptionResult(
			option.id(),
			option.name(),
			option.description(),
			option.price(),
			option.priceType().name(),
			option.durationMinutes(),
			option.visible(),
			option.sortOrder(),
			assignments.stream()
				.map(assignment -> new SpecialistPriceResult(
					assignment.specialist().id(),
					assignment.specialist().name(),
					assignment.priceOverride(),
					assignment.priceTypeOverride() == null ? null : assignment.priceTypeOverride().name(),
					assignment.effectivePrice(),
					assignment.effectivePriceType().name()
				))
				.toList(),
			option.createdAt(),
			option.updatedAt()
		);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record ValidatedOption(
		String name,
		String description,
		BigDecimal price,
		PartnerPriceType priceType,
		Integer durationMinutes,
		boolean visible,
		int sortOrder,
		List<SpecialistPriceCommand> specialists
	) {
	}
}
