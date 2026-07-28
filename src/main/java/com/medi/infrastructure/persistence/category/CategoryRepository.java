package com.medi.infrastructure.persistence.category;

import com.medi.domain.category.Category;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findByIdIn(Collection<Long> ids);
}
