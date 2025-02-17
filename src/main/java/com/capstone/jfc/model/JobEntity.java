package com.capstone.jfc.model;

import com.capstone.jfc.enums.JobCategory;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobId;    // e.g. eventId from the event

    @Enumerated(EnumType.STRING)
    private JobCategory jobCategory;

    private String tenantId;

    @Lob
    private String payload;  // raw JSON from the event
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Instant timestampCreated;
    private Instant timestampUpdated;

    public JobEntity() {}

    // getters & setters
    // ...
    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }
    public void setJobCategory(JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }

    public JobStatus getStatus() {
        return status;
    }
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getTimestampCreated() {
        return timestampCreated;
    }
    public void setTimestampCreated(Instant timestampCreated) {
        this.timestampCreated = timestampCreated;
    }

    public Instant getTimestampUpdated() {
        return timestampUpdated;
    }
    public void setTimestampUpdated(Instant timestampUpdated) {
        this.timestampUpdated = timestampUpdated;
    }
}
