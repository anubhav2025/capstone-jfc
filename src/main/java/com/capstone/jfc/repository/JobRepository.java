package com.capstone.jfc.repository;

import com.capstone.jfc.enums.JobCategory;
import com.capstone.jfc.model.JobEntity;
import com.capstone.jfc.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {

    List<JobEntity> findByJobCategoryAndStatus(JobCategory jobCategory, JobStatus status);

    JobEntity findByJobId(String jobId);
}
