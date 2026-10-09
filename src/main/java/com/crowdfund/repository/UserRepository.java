package com.crowdfund.repository;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    java.util.List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchByNameOrEmail(@Param("search") String search, Pageable pageable);

    long countByRole(Role role);

    long countByActive(boolean active);
}
