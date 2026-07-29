package com.medi.application.doctor;

import com.medi.application.media.MediaLifecycleService;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorLifecycleService {

	private final DoctorRepository doctorRepository;
	private final MediaLifecycleService mediaLifecycleService;

	public DoctorLifecycleService(DoctorRepository doctorRepository, MediaLifecycleService mediaLifecycleService) {
		this.doctorRepository = doctorRepository;
		this.mediaLifecycleService = mediaLifecycleService;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDeleteByHospital(Long hospitalId) {
		for (Doctor doctor : doctorRepository.findByHospital_IdAndDeletedAtIsNull(hospitalId)) {
			doctor.softDelete();
			mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.DOCTOR, doctor.id());
		}
	}
}
