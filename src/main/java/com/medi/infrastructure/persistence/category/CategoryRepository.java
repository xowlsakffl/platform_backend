package com.medi.infrastructure.persistence.category;

import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryDomain;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

	List<Category> findByIdIn(Collection<Long> ids);

	List<Category> findAllByDomain(CategoryDomain domain, Sort sort);

	Optional<Category> findByDomainAndCode(CategoryDomain domain, String code);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Category> findForUpdateById(Long id);

	List<Category> findByDomainAndFullPathStartingWithOrderByDepthAsc(CategoryDomain domain, String fullPathPrefix);

	boolean existsByDomainAndCode(CategoryDomain domain, String code);

	boolean existsByDomainAndCodeAndIdNot(CategoryDomain domain, String code, Long id);

	boolean existsByDomainAndParentIsNullAndName(CategoryDomain domain, String name);

	boolean existsByDomainAndParentIsNullAndNameAndIdNot(CategoryDomain domain, String name, Long id);

	boolean existsByDomainAndParent_IdAndName(CategoryDomain domain, Long parentId, String name);

	boolean existsByDomainAndParent_IdAndNameAndIdNot(CategoryDomain domain, Long parentId, String name, Long id);

	boolean existsByParent_Id(Long parentId);
}
