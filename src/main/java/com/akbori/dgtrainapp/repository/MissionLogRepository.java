package com.akbori.dgtrainapp.repository;

import com.akbori.dgtrainapp.model.MissionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MissionLogRepository extends JpaRepository<MissionLog, Long> {
    List<MissionLog> findByGameIdOrderByIdDesc(String gameId);
}