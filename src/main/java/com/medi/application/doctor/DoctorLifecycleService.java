package com.medi.application.doctor;

import com.medi.application.media.MediaLifecycleService;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.media.MediaOwnerType;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorLifecycleService {

	private final DoctorRepository doctorRepository;
	private final MediaLifecycleService mediaLifecycleService;
	private final CategoryAssignmentRepository categoryAssignmentRepository;

	public DoctorLifecycleService(
		DoctorRepository doctorRepository,
		MediaLifecycleService mediaLifecycleService,
		CategoryAssignmentRepository categoryAssignmentRepository
	) {
		this.doctorRepository = doctorRepository;
		this.mediaLifecycleService = mediaLifecycleService;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDeleteByHospital(Long hospitalId) {
		for (Doctor doctor : doctorRepository.findByHospital_IdAndDeletedAtIsNull(hospitalId)) {
			softDelete(doctor);
		}
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void softDelete(Doctor doctor) {
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.DOCTOR, doctor.id());
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.DOCTOR_TARGET_TYPE,
			doctor.id()
		);
		categoryAssignmentRepository.flush();
		doctor.softDelete();
	}
}
