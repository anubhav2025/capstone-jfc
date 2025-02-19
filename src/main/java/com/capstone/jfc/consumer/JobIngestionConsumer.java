package com.capstone.jfc.consumer;

import com.capstone.jfc.dto.event.ParseRequestEvent;
import com.capstone.jfc.dto.event.ScanRequestEvent;
import com.capstone.jfc.dto.event.StateUpdateJobEvent;
import com.capstone.jfc.dto.event.payload.ParseRequestEventPayload;
import com.capstone.jfc.dto.event.payload.ScanRequestEventPayload;
import com.capstone.jfc.dto.event.payload.StateUpdateJobEventPayload;
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
 * A single JFC consumer that listens on "job_ingestion" for 
 * multiple event types from any service:
 *  - SCAN_REQUEST (Auth, etc.)
 *  - PARSE_REQUEST (Tool scheduler)
 *  - UPDATE_FINDING (Auth)
 *  - etc.
 */
@Component
public class JobIngestionConsumer {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public JobIngestionConsumer(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(
        topics = "${topics.job_ingestion_topic}", // e.g. "job-ingestion"
        groupId = "jfc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIngestion(@Payload String message) {
        try {
            System.out.println("[JobIngestionConsumer] Raw message => " + message);

            // 1) parse top-level
            JsonNode firstRoot = objectMapper.readTree(message);
            JsonNode realRoot = firstRoot;

            // if double-serialized => unwrap
            if (firstRoot.isTextual()) {
                String actualJson = firstRoot.asText();
                System.out.println("[JobIngestionConsumer] Double-serialized => " + actualJson);
                realRoot = objectMapper.readTree(actualJson);
            }

            if (!realRoot.has("type")) {
                System.err.println("[JobIngestionConsumer] Missing 'type' => ignoring.");
                return;
            }

            String typeStr = realRoot.get("type").asText();
            switch (typeStr) {
                case "SCAN_REQUEST":
                    handleScanRequest(realRoot);
                    break;
                case "PARSE_REQUEST":
                    handleParseRequest(realRoot);
                    break;
                case "UPDATE_FINDING":
                    handleUpdateFinding(realRoot);
                    break;
                default:
                    System.out.println("[JobIngestionConsumer] Received type=" + typeStr 
                        + ", ignoring (not recognized).");
                    break;
            }

        } catch (Exception e) {
            System.err.println("[JobIngestionConsumer] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleScanRequest(JsonNode root) throws Exception {
        ScanRequestEvent event = objectMapper.treeToValue(root, ScanRequestEvent.class);
        ScanRequestEventPayload payload = event.getPayload();

        // map tool => SCAN_PULL_*
        JobCategory cat = mapToolToPullCategory(payload.getTool());
        String jobId = event.getEventId();
        String tenantId = payload.getTenantId();

        // store only the payload
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
        System.out.println("[JobIngestionConsumer] SCAN_REQUEST => jobId=" + jobId
            + ", category=" + cat + ", tenant=" + tenantId);
    }

    private void handleParseRequest(JsonNode root) throws Exception {
        ParseRequestEvent event = objectMapper.treeToValue(root, ParseRequestEvent.class);
        ParseRequestEventPayload payload = event.getPayload();

        // map tool => SCAN_PARSE_*
        JobCategory cat = mapToolToParseCategory(payload.getTool());
        String jobId = event.getEventId();
        String tenantId = payload.getTenantId();

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
        System.out.println("[JobIngestionConsumer] PARSE_REQUEST => jobId=" + jobId
            + ", category=" + cat + ", tenant=" + tenantId);
    }

    private void handleUpdateFinding(JsonNode root) throws Exception {
        StateUpdateJobEvent event = objectMapper.treeToValue(root, StateUpdateJobEvent.class);
        StateUpdateJobEventPayload payload = event.getPayload();

        String jobId = event.getEventId();
        String tenantId = payload.getTenantId();

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
        System.out.println("[JobIngestionConsumer] UPDATE_FINDING => jobId=" + jobId
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

    private JobCategory mapToolToParseCategory(ToolTypes tool) {
        switch (tool) {
            case CODE_SCAN:
                return JobCategory.SCAN_PARSE_CODESCAN;
            case DEPENDABOT:
                return JobCategory.SCAN_PARSE_DEPENDABOT;
            case SECRET_SCAN:
                return JobCategory.SCAN_PARSE_SECRETSCAN;
            default:
                throw new IllegalArgumentException("Unknown tool: " + tool);
        }
    }
}
