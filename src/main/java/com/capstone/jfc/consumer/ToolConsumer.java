package com.capstone.jfc.consumer;

import com.capstone.jfc.dto.event.ParseRequestEvent;
import com.capstone.jfc.dto.event.payload.ParseRequestEventPayload;
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

@Component
public class ToolConsumer {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public ToolConsumer(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(
        topics = "${topics.jfc_tool}", // e.g. "jfc_tool"
        groupId = "jfc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTool(@Payload String message) {
        try {
            System.out.println("[ToolConsumer] Raw message: " + message);

            // minimal parse
            JsonNode firstRoot = objectMapper.readTree(message);
            JsonNode realRoot = firstRoot;
            if (firstRoot.isTextual()) {
                String actualJson = firstRoot.asText();
                realRoot = objectMapper.readTree(actualJson);
            }

            if (!realRoot.has("type")) {
                System.err.println("[ToolConsumer] Missing 'type' => ignoring.");
                return;
            }
            String typeStr = realRoot.get("type").asText();
            if (!EventTypes.PARSE_REQUEST.name().equals(typeStr)) {
                System.out.println("[ToolConsumer] Not PARSE_REQUEST => ignoring.");
                return;
            }

            // parse full => get eventId, etc.
            ParseRequestEvent event = objectMapper.treeToValue(realRoot, ParseRequestEvent.class);
            ParseRequestEventPayload payloadObj = event.getPayload();

            // build jobCategory => SCAN_PARSE_XXX
            JobCategory cat = mapToolToParseCategory(payloadObj.getTool());
            String jobId = event.getEventId();
            String tenantId = payloadObj.getTenantId();

            // store only the parse payload
            String payloadJson = objectMapper.writeValueAsString(payloadObj);

            JobEntity job = new JobEntity();
            job.setJobId(jobId);
            job.setJobCategory(cat);
            job.setTenantId(tenantId);
            job.setPayload(payloadJson); // only payload
            job.setStatus(JobStatus.NEW);
            job.setTimestampCreated(Instant.now());
            job.setTimestampUpdated(Instant.now());
            jobRepository.save(job);

            System.out.println("[ToolConsumer] Created parse job => jobId=" + jobId
                + ", category=" + cat + ", tenant=" + tenantId
                + ", filePath=" + payloadObj.getFilePath());

        } catch (Exception e) {
            System.err.println("[ToolConsumer] Error: " + e.getMessage());
            e.printStackTrace();
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
