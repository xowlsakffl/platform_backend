package com.platform.infrastructure.persistence.specialist;

import com.platform.domain.specialist.SpecialistOption;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpecialistOptionRepository extends JpaRepository<SpecialistOption, Long> {

	@EntityGraph(attributePaths = "specialist")
	List<SpecialistOption> findByPartnerOption_IdAndSpecialist_DeletedAtIsNullOrderBySpecialist_SortOrderAscSpecialist_IdAsc(
		Long partnerOptionId
	);

	@EntityGraph(attributePaths = {"specialist", "partnerOption"})
	List<SpecialistOption> findByPartnerOption_IdInAndSpecialist_DeletedAtIsNull(Collection<Long> partnerOptionIds);

	@EntityGraph(attributePaths = {"specialist", "partnerOption"})
	List<SpecialistOption> findBySpecialist_IdOrderByPartnerOption_SortOrderAscPartnerOption_IdAsc(Long specialistId);

	@Query("""
		select assignment.specialist.id as specialistId, count(assignment.id) as itemCount
		from SpecialistOption assignment
		where assignment.specialist.id in :specialistIds
		group by assignment.specialist.id
		""")
	List<SpecialistOptionCount> countBySpecialistIds(Collection<Long> specialistIds);

	void deleteByPartnerOption_Id(Long partnerOptionId);

	void deleteBySpecialist_Id(Long specialistId);
}
