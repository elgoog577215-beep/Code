package com.onlinejudge.identity.persistence;

import com.onlinejudge.identity.domain.TeacherAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherAccountRepository extends JpaRepository<TeacherAccount, UUID> {
    Optional<TeacherAccount> findByUsernameNormalized(String usernameNormalized);
    boolean existsByUsernameNormalized(String usernameNormalized);
    List<TeacherAccount> findByStatusOrderByCreatedAtAsc(TeacherAccount.Status status);

    default long countByStatus(TeacherAccount.Status status) {
        return findByStatusOrderByCreatedAtAsc(status).size();
    }
    long countByRoleAndStatus(TeacherAccount.Role role, TeacherAccount.Status status);
}
