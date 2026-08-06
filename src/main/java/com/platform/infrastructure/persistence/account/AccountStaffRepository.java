package com.platform.infrastructure.persistence.account;

import com.platform.domain.account.AccountStaff;
import com.platform.domain.account.AccountStaffStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountStaffRepository extends JpaRepository<AccountStaff, Long> {

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByEmailAndDeletedAtIsNull(String email);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByLoginIdAndDeletedAtIsNull(String loginId);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByEmail(String email);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<AccountStaff> findByLoginId(String loginId);

	boolean existsByNickname(String nickname);

	Optional<AccountStaff> findByIdAndDeletedAtIsNull(Long id);

	Optional<AccountStaff> findFirstByStatusAndDeletedAtIsNullOrderByIdAsc(AccountStaffStatus status);

	@Query("""
		select staff
		from AccountStaff staff
		where staff.deletedAt is null
		  and staff.status = com.platform.domain.account.AccountStaffStatus.ACTIVE
		  and (
			:q is null
			or lower(staff.name) like :q
			or lower(staff.nickname) like :q
			or lower(staff.email) like :q
		  )
		order by staff.name asc, staff.id asc
		""")
	List<AccountStaff> searchActiveOptions(@Param("q") String query);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	@Query("select staff from AccountStaff staff where staff.id = :id and staff.deletedAt is null")
	Optional<AccountStaff> findForAuthentication(@Param("id") Long id);
}
