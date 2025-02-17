package com.capstone.jfc.service;

import com.capstone.jfc.enums.JobCategory;
import com.capstone.jfc.model.JobEntity;
import com.capstone.jfc.model.JobStatus;
import com.capstone.jfc.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class JobSchedulerService {

    private final JobRepository jobRepository;
    private final ConcurrencyConfigService concurrencyService;
    private final JfcProducerService producer;
    private final ReentrantLock lock = new ReentrantLock(true);

    public JobSchedulerService(JobRepository jobRepository,
                               ConcurrencyConfigService concurrencyService,
                               JfcProducerService producer) {
        this.jobRepository = jobRepository;
        this.concurrencyService = concurrencyService;
        this.producer = producer;
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledJobPicking() {
        scheduleJobs();
    }

    @Transactional
    public void scheduleJobs() {
        lock.lock();
        try {
            var categories = List.of(
                JobCategory.SCAN_PULL_CODESCAN,
                JobCategory.SCAN_PULL_DEPENDABOT,
                JobCategory.SCAN_PULL_SECRETSCAN,
                JobCategory.SCAN_PARSE_CODESCAN,
                JobCategory.SCAN_PARSE_DEPENDABOT,
                JobCategory.SCAN_PARSE_SECRETSCAN,
                JobCategory.UPDATE_FINDING
            );

            for (JobCategory cat : categories) {
                List<JobEntity> inProgress = jobRepository.findByJobCategoryAndStatus(cat, JobStatus.IN_PROGRESS);
                int currentInProgress = inProgress.size();
                int maxForCategory = concurrencyService.getMaxJobsForCategory(cat);
                int canScheduleCategory = maxForCategory - currentInProgress;
                if (canScheduleCategory <= 0) continue;

                List<JobEntity> newJobs = jobRepository.findByJobCategoryAndStatus(cat, JobStatus.NEW);
                newJobs.sort(Comparator.comparing(JobEntity::getTimestampCreated));

                Map<String,Integer> tenantCount = new HashMap<>();
                for (JobEntity j : inProgress) {
                    tenantCount.merge(j.getTenantId(), 1, Integer::sum);
                }

                List<JobEntity> selected = new ArrayList<>();
                for (JobEntity job : newJobs) {
                    if (selected.size() >= canScheduleCategory) break;
                    String tenantId = job.getTenantId();
                    int maxTenant = concurrencyService.getMaxJobsForTenant(tenantId);
                    int tcount = tenantCount.getOrDefault(tenantId, 0);

                    if (tcount < maxTenant) {
                        selected.add(job);
                        tenantCount.put(tenantId, tcount + 1);
                    }
                }

                // System.out.println(selected.size());

                for (JobEntity job : selected) {
                    job.setStatus(JobStatus.READY);
                    job.setTimestampUpdated(Instant.now());
                    jobRepository.save(job);

                    // pass jobId + jobPayload => rebuild event
                    producer.publishJob(job);
                    System.out.println("publishing Job....");

                    job.setStatus(JobStatus.IN_PROGRESS);
                    job.setTimestampUpdated(Instant.now());
                    jobRepository.save(job);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public void acknowledgeJob(String jobId, boolean success) {
        lock.lock();
        try {
            var job = jobRepository.findByJobId(jobId);
            if (job == null) {
                System.err.println("No job found for jobId=" + jobId);
                return;
            }
            job.setStatus(success ? JobStatus.SUCCESS : JobStatus.FAILURE);
            job.setTimestampUpdated(Instant.now());
            jobRepository.save(job);
        } finally {
            lock.unlock();
        }
        scheduleJobs();
    }
}
