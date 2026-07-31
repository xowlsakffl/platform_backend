package com.medi.application.partner;

import com.medi.application.auth.PermissionService;
import com.medi.application.partner.query.SearchPartnerFeaturesForStaffQuery;
import com.medi.application.partner.result.PartnerFeatureResult;
import com.medi.common.security.AccessPermissions;
import com.medi.common.security.AuthenticatedActor;
import com.medi.domain.partner.PartnerFeature;
import com.medi.infrastructure.persistence.partner.PartnerFeatureRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerFeatureForStaffService {

	private final PermissionService permissionService;
	private final PartnerFeatureRepository repository;

	public PartnerFeatureForStaffService(PermissionService permissionService, PartnerFeatureRepository repository) {
		this.permissionService = permissionService;
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<PartnerFeatureResult> list(
		AuthenticatedActor actor,
		SearchPartnerFeaturesForStaffQuery query
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);

		Specification<PartnerFeature> specification = (root, ignored, builder) -> builder.conjunction();
		if (StringUtils.hasText(query.q())) {
			String keyword = "%" + query.q().trim() + "%";
			specification = specification.and((root, ignored, builder) -> builder.or(
				builder.like(root.get("name"), keyword),
				builder.like(root.get("code"), keyword)
			));
		}
		if (query.status() != null && !query.status().isEmpty()) {
			specification = specification.and((root, ignored, builder) -> root.get("status").in(query.status()));
		}

		Sort.Direction direction = Sort.Direction.fromString(query.direction());
		Sort sort = Sort.by(direction, sortProperty(query.sort())).and(Sort.by(Sort.Direction.ASC, "id"));
		return repository.findAll(specification, sort).stream()
			.map(this::toResult)
			.toList();
	}

	private String sortProperty(String sort) {
		return "sort_order".equals(sort) ? "sortOrder" : sort;
	}

	private PartnerFeatureResult toResult(PartnerFeature feature) {
		return new PartnerFeatureResult(
			feature.id(),
			feature.code(),
			feature.name(),
			feature.sortOrder(),
			feature.status().name()
		);
	}
}
