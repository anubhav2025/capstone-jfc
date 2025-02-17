// package com.capstone.jfc.consumer;

// import com.capstone.jfc.dto.event.ParseRequestEvent;
// import com.capstone.jfc.dto.event.payload.ParseRequestEventPayload;
// import com.capstone.jfc.enums.JobCategory;
// import com.capstone.jfc.enums.ToolTypes;
// import com.capstone.jfc.model.JobEntity;
// import com.capstone.jfc.model.JobStatus;
// import com.capstone.jfc.repository.JobRepository;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Component;

// import java.time.Instant;

// @Component
// public class ParserConsumer {

//     private final JobRepository jobRepository;
//     private final ObjectMapper objectMapper;

//     public ParserConsumer(JobRepository jobRepository) {
//         this.jobRepository = jobRepository;
//         this.objectMapper = new ObjectMapper();
//     }

//     @KafkaListener(
//         topics = "#{${topics.jfc_parse}}",
//         groupId = "jfc-group",
//         containerFactory = "kafkaListenerContainerFactory"
//     )
//     public void consumeParserEvent(@Payload String message) {
//         try {
//             ParseRequestEvent event = objectMapper.readValue(message, ParseRequestEvent.class);
//             ParseRequestEventPayload payload = event.getPayload();

//             String jobId = event.getEventId();
//             JobCategory cat = mapToolToParseCategory(payload.getTool());
//             String tenantId = payload.getTenantId();

//             // create a new job => NEW
//             JobEntity job = new JobEntity();
//             job.setJobId(jobId);
//             job.setJobCategory(cat);
//             job.setTenantId(tenantId);
//             job.setPayload(message);
//             job.setStatus(JobStatus.NEW);
//             job.setTimestampCreated(Instant.now());
//             job.setTimestampUpdated(Instant.now());

//             jobRepository.save(job);
//             System.out.println("[ParserConsumer] Created parse job => jobId=" + jobId 
//                 + ", category=" + cat + ", tenant=" + tenantId);

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     private JobCategory mapToolToParseCategory(ToolTypes tool) {
//         switch (tool) {
//             case CODE_SCAN: return JobCategory.SCAN_PARSE_CODESCAN;
//             case DEPENDABOT: return JobCategory.SCAN_PARSE_DEPENDABOT;
//             case SECRET_SCAN: return JobCategory.SCAN_PARSE_SECRETSCAN;
//             default: throw new IllegalArgumentException("Unknown tool: " + tool);
//         }
//     }
// }
