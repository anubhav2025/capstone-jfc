package com.capstone.jfc.consumer;

import com.capstone.jfc.dto.event.ScanRequestEvent;
import com.capstone.jfc.dto.event.StateUpdateJobEvent;
import com.capstone.jfc.dto.event.payload.ScanRequestEventPayload;
import com.capstone.jfc.dto.event.payload.StateUpdateJobEventPayload;
import com.capstone.jfc.enums.EventTypes;
import com.capstone.jfc.enums.JobCategory;
import com.capstone.jfc.enums.ToolTypes;
import com.capstone.jfc.model.JobEntity;
import com.capstone.jfc.model.JobStatus;
import com.capstone.jfc.repository.JobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * A single JFC consumer that listens on "jfc_auth" for 
 * all event types from the Auth server:
 * - SCAN_REQUEST => store job with SCAN_PULL_...
 * - UPDATE_FINDING => store job with UPDATE_FINDING
 * - else => ignore
 */
@Component
public class AuthConsumer {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public AuthConsumer(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(
        topics = "${topics.jfc_auth}",    // e.g. "jfc_auth"
        groupId = "jfc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuth(@Payload String message) {
        try {
            System.out.println("[AuthConsumer] Raw message => " + message);

            // 1) parse top-level
            JsonNode firstRoot = objectMapper.readTree(message);
            JsonNode realRoot = firstRoot;

            // Unwrap if double-serialized
            if (firstRoot.isTextual()) {
                String actualJson = firstRoot.asText();
                System.out.println("[AuthConsumer] Double-serialized => " + actualJson);
                realRoot = objectMapper.readTree(actualJson);
            }

            // 2) Check "type"
            if (!realRoot.has("type")) {
                System.err.println("[AuthConsumer] Missing 'type' => ignoring.");
                return;
            }
            String typeStr = realRoot.get("type").asText();

            // We handle SCAN_REQUEST or UPDATE_FINDING
            if (typeStr.equals(EventTypes.SCAN_REQUEST.name())) {
                handleScanRequest(realRoot);
            } else if (typeStr.equals(EventTypes.UPDATE_FINDING.name())) {
                handleStateUpdate(realRoot);
            } else {
                System.out.println("[AuthConsumer] Received type=" + typeStr 
                    + ", ignoring. (No other logic here.)");
            }

        } catch (Exception e) {
            System.err.println("[AuthConsumer] Error parsing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * If "type=SCAN_REQUEST", parse as ScanRequestEvent => store job with 
     * SCAN_PULL_* category based on the tool (CODE_SCAN,DEPENDABOT,SECRET_SCAN).
     */
    private void handleScanRequest(JsonNode realRoot) throws Exception {
        // parse event fully
        ScanRequestEvent event = objectMapper.treeToValue(realRoot, ScanRequestEvent.class);
        ScanRequestEventPayload payload = event.getPayload();

        // map tool => SCAN_PULL_* category
        JobCategory cat = mapToolToPullCategory(payload.getTool());
        String jobId = event.getEventId();
        String tenantId = payload.getTenantId();

        // If you only want to store the payload in DB
        String payloadJson = objectMapper.writeValueAsString(payload);

        JobEntity job = new JobEntity();
        job.setJobId(jobId);
        job.setJobCategory(cat);
        job.setTenantId(tenantId);
        job.setPayload(payloadJson);
        job.setStatus(JobStatus.NEW);
        job.setTimestampCreated(Instant.now());
        job.setTimestampUpdated(Instant.now());

        jobRepository.save(job);
        System.out.println("[AuthConsumer] SCAN_REQUEST => jobId=" + jobId 
            + ", category=" + cat + ", tenant=" + tenantId);
    }

    /**
     * If "type=UPDATE_FINDING", parse as StateUpdateJobEvent => store job with 
     * category=UPDATE_FINDING
     */
    private void handleStateUpdate(JsonNode realRoot) throws Exception {
        StateUpdateJobEvent event = objectMapper.treeToValue(realRoot, StateUpdateJobEvent.class);
        StateUpdateJobEventPayload payload = event.getPayload();

        String jobId = event.getEventId();
        String tenantId = payload.getTenantId();

        // store only the payload
        String payloadJson = objectMapper.writeValueAsString(payload);

        JobEntity job = new JobEntity();
        job.setJobId(jobId);
        job.setJobCategory(JobCategory.UPDATE_FINDING);
        job.setTenantId(tenantId);
        job.setPayload(payloadJson);
        job.setStatus(JobStatus.NEW);
        job.setTimestampCreated(Instant.now());
        job.setTimestampUpdated(Instant.now());

        jobRepository.save(job);
        System.out.println("[AuthConsumer] UPDATE_FINDING => jobId=" + jobId 
            + ", category=UPDATE_FINDING, tenant=" + tenantId);
    }

    private JobCategory mapToolToPullCategory(ToolTypes tool) {
        switch (tool) {
            case CODE_SCAN:
                return JobCategory.SCAN_PULL_CODESCAN;
            case DEPENDABOT:
                return JobCategory.SCAN_PULL_DEPENDABOT;
            case SECRET_SCAN:
                return JobCategory.SCAN_PULL_SECRETSCAN;
            default:
                throw new IllegalArgumentException("Unknown tool: " + tool);
        }
    }
}
