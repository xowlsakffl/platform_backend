package com.medi.application.specialist;

import com.medi.application.media.MediaLifecycleService;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.specialist.Specialist;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.specialist.SpecialistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistLifecycleService {

	private final SpecialistRepository specialistRepository;
	private final MediaLifecycleService mediaLifecycleService;
	private final CategoryAssignmentRepository categoryAssignmentRepository;

	public SpecialistLifecycleService(
		SpecialistRepository specialistRepository,
		MediaLifecycleService mediaLifecycleService,
		CategoryAssignmentRepository categoryAssignmentRepository
	) {
		this.specialistRepository = specialistRepository;
		this.mediaLifecycleService = mediaLifecycleService;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDeleteByPartner(Long partnerId) {
		for (Specialist specialist : specialistRepository.findByPartner_IdAndDeletedAtIsNull(partnerId)) {
			softDelete(specialist);
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDelete(Specialist specialist) {
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.SPECIALIST, specialist.id());
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.SPECIALIST_TARGET_TYPE,
			specialist.id()
		);
		categoryAssignmentRepository.flush();
		specialist.softDelete();
	}
}
