package com.capstone.jfc.consumer;

import com.capstone.jfc.dto.ack.AcknowledgementStatus;
import com.capstone.jfc.service.JobSchedulerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class JobAckConsumer {

    private final JobSchedulerService scheduler;
    private final ObjectMapper objectMapper;

    public JobAckConsumer(JobSchedulerService scheduler) {
        this.scheduler = scheduler;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(
        topics = "${topics.job_ack}",
        groupId = "jfc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAck(@Payload String message) {
        try {
            System.out.println("[JobAckConsumer] Raw ack message => " + message);

            // 1) Parse the raw JSON
            JsonNode root = objectMapper.readTree(message);

            // 2) Check for "payload"
            if (!root.has("payload")) {
                System.err.println("[JobAckConsumer] No 'payload' in ack => ignoring.");
                return;
            }
            JsonNode payloadNode = root.get("payload");

            // 3) Check for "jobId"
            if (!payloadNode.has("jobId")) {
                System.err.println("[JobAckConsumer] No 'jobId' in payload => ignoring.");
                return;
            }
            String jobId = payloadNode.get("jobId").asText();

            // 4) Check for "status" => default to SUCCESS if missing
            String statusStr = (payloadNode.has("status"))
                ? payloadNode.get("status").asText()
                : AcknowledgementStatus.SUCCESS.toString();

            System.out.println("Status : " + statusStr);
            // System.out.println(AcknowledgementStatus.SUCCESS.name());

            boolean success = statusStr.equalsIgnoreCase(AcknowledgementStatus.SUCCESS.toString());

            // 5) Acknowledge the job in the scheduler
            scheduler.acknowledgeJob(jobId, success);
            System.out.println("[JobAckConsumer] Ack => jobId=" + jobId + ", success=" + success);

        } catch (Exception e) {
            System.err.println("[JobAckConsumer] Error parsing ack: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
