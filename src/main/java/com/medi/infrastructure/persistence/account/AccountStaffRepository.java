package com.medi.infrastructure.persistence.account;

import com.medi.domain.account.AccountStaff;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountStaffRepository extends JpaRepository<AccountStaff, Long> {

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByEmailAndDeletedAtIsNull(String email);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByEmail(String email);

	boolean existsByNickname(String nickname);

	Optional<AccountStaff> findByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	@Query("select staff from AccountStaff staff where staff.id = :id and staff.deletedAt is null")
	Optional<AccountStaff> findForAuthentication(@Param("id") Long id);
}
