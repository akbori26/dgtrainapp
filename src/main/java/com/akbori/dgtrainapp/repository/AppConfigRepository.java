package com.akbori.dgtrainapp.repository;

import com.akbori.dgtrainapp.model.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {}