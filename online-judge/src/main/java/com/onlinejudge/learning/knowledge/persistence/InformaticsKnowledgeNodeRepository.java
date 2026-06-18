package com.onlinejudge.learning.knowledge.persistence;

import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InformaticsKnowledgeNodeRepository extends JpaRepository<InformaticsKnowledgeNode, Long> {

    boolean existsByCode(String code);

    Optional<InformaticsKnowledgeNode> findByCode(String code);

    List<InformaticsKnowledgeNode> findByCodeIn(Collection<String> codes);

    List<InformaticsKnowledgeNode> findByEnabledTrueOrderByPathAscSortOrderAscCodeAsc();

    List<InformaticsKnowledgeNode> findAllByOrderByPathAscSortOrderAscCodeAsc();
}
