package com.platform.application.specialist;

import com.platform.application.media.MediaLifecycleService;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.media.MediaOwnerType;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistLifecycleService {

	private final SpecialistRepository specialistRepository;
	private final MediaLifecycleService mediaLifecycleService;

	public SpecialistLifecycleService(
		SpecialistRepository specialistRepository,
		MediaLifecycleService mediaLifecycleService
	) {
		this.specialistRepository = specialistRepository;
		this.mediaLifecycleService = mediaLifecycleService;
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
		specialist.softDelete();
	}
}
