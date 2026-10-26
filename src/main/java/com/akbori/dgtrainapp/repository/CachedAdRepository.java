package com.akbori.dgtrainapp.repository;

import com.akbori.dgtrainapp.model.CachedAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface CachedAdRepository extends JpaRepository<CachedAd, CachedAd.Key> {
    List<CachedAd> findByGameIdOrderByDisplayIndexAsc(String gameId);

    Optional<CachedAd> findByGameIdAndAdId(String gameId, String adId);

    // Derived deletes need a transaction
    @Transactional
    void deleteByGameId(String gameId);
}
