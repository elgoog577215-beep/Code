package com.onlinejudge.learning.standardlibrary.persistence;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardSkillUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiStandardSkillUnitRepository extends JpaRepository<AiStandardSkillUnit, Long> {
    boolean existsByCode(String code);

    Optional<AiStandardSkillUnit> findByCode(String code);

    List<AiStandardSkillUnit> findByEnabledTrueOrderByCategoryAscCodeAsc();

    List<AiStandardSkillUnit> findByEnabledTrueAndPrimaryKnowledgeNodeCodeOrderByCategoryAscCodeAsc(
            String primaryKnowledgeNodeCode);
}
