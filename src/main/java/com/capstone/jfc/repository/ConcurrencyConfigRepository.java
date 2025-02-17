package com.capstone.jfc.repository;

import com.capstone.jfc.model.ConcurrencyConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcurrencyConfigRepository extends JpaRepository<ConcurrencyConfigEntity, String> {
    // The primary key is configKey (String)
}
