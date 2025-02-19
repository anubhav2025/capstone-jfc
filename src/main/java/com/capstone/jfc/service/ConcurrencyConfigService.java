package com.capstone.jfc.service;

import com.capstone.jfc.enums.JobCategory;
import com.capstone.jfc.model.ConcurrencyConfigEntity;
import com.capstone.jfc.repository.ConcurrencyConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class ConcurrencyConfigService {

    private final ConcurrencyConfigRepository repository;

    public ConcurrencyConfigService(ConcurrencyConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * For job category concurrency:
     * we do findById(cat.name()) => if not found => default=5
     */
    public int getMaxJobsForCategory(JobCategory category) {
        String key = category.name();
        return repository.findById(key)
                .map(ConcurrencyConfigEntity::getConfigValue)
                .orElseThrow(() -> new RuntimeException(
                    "No concurrency config found for jobCategory=" + key
                ));
    }

    /**
     * For tenant concurrency:
     * findById(tenantId) => default=2
     */
    public int getMaxJobsForTenant(String tenantId) {
        return repository.findById(tenantId)
                .map(ConcurrencyConfigEntity::getConfigValue)
                .orElse(2);
    }
}
