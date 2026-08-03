package com.platform.infrastructure.persistence.specialist;

import com.platform.domain.specialist.SpecialistOption;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistOptionRepository extends JpaRepository<SpecialistOption, Long> {

	@EntityGraph(attributePaths = "specialist")
	List<SpecialistOption> findByPartnerOption_IdOrderBySpecialist_SortOrderAscSpecialist_IdAsc(Long partnerOptionId);

	@EntityGraph(attributePaths = {"specialist", "partnerOption"})
	List<SpecialistOption> findByPartnerOption_IdIn(Collection<Long> partnerOptionIds);

	void deleteByPartnerOption_Id(Long partnerOptionId);
}
