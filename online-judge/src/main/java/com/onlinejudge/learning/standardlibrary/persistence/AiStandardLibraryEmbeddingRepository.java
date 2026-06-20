package com.onlinejudge.learning.standardlibrary.persistence;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryEmbedding;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiStandardLibraryEmbeddingRepository extends JpaRepository<AiStandardLibraryEmbedding, Long> {

    Optional<AiStandardLibraryEmbedding> findByItemAndEmbeddingModel(AiStandardLibraryItem item, String embeddingModel);

    List<AiStandardLibraryEmbedding> findByEmbeddingModelAndStatusOrderByUpdatedAtAsc(String embeddingModel, String status);
}
