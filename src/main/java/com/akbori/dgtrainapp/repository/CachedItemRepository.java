package com.akbori.dgtrainapp.repository;

import com.akbori.dgtrainapp.model.CachedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface CachedItemRepository extends JpaRepository<CachedItem, CachedItem.Key> {
    List<CachedItem> findByGameIdOrderByDisplayIndexAsc(String gameId);

    Optional<CachedItem> findByGameIdAndItemId(String gameId, String itemId);

    // Derived deletes need a transaction
    @Transactional
    void deleteByGameId(String gameId);
}
