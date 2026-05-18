package com.onlinejudge.classroom.persistence;

import com.onlinejudge.classroom.domain.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    List<ClassGroup> findAllByOrderByCreatedAtDesc();
    Optional<ClassGroup> findByNameIgnoreCase(String name);
}
