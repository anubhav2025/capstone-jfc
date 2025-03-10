package com.capstone.jfc.service;

import com.capstone.jfc.dto.event.*;
import com.capstone.jfc.dto.event.payload.CreateTicketEventPayload;
import com.capstone.jfc.dto.event.payload.ParseRequestEventPayload;
import com.capstone.jfc.dto.event.payload.RunbookJobEventPayload;
import com.capstone.jfc.dto.event.payload.ScanRequestEventPayload;
import com.capstone.jfc.dto.event.payload.StateUpdateJobEventPayload;
import com.capstone.jfc.dto.event.payload.UpdateTicketEventPayload;
import com.capstone.jfc.enums.EventTypes;
import com.capstone.jfc.enums.JobCategory;
import com.capstone.jfc.model.JobEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class JfcProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${topics.jfc_tool:jfc_tool}")
    private String toolTopic;

    @Value("${topics.jfc_parser}")
    private String parseTopic;

    @Value("${topics.bg_jobs:bg-jobs}")
    private String bgJobsTopic;  // new for "bgJobs" microservice

    public JfcProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishJob(JobEntity job) {
        try {
            JobCategory jobCategory = job.getJobCategory();
            String payloadJson = job.getPayload();
            String jobId = job.getJobId();

            if (jobCategory.name().startsWith("SCAN_PULL")) {
                // parse as ScanRequestEventPayload => produce to jfc_tool
                ScanRequestEventPayload payloadObj =
                        objectMapper.readValue(payloadJson, ScanRequestEventPayload.class);
                ScanRequestEvent event = new ScanRequestEvent(payloadObj);
                event.setEventId(jobId);

                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(toolTopic, eventJson);

            } else if (jobCategory.name().startsWith("SCAN_PARSE")) {
                // parse as ParseRequestEventPayload => produce to parseTopic
                ParseRequestEventPayload payloadObj =
                        objectMapper.readValue(payloadJson, ParseRequestEventPayload.class);
                ParseRequestEvent event = new ParseRequestEvent(payloadObj);
                event.setEventId(jobId);

                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(parseTopic, eventJson);

            } else if (jobCategory == JobCategory.UPDATE_FINDING) {
                // 1) parse StateUpdateJobEventPayload
                StateUpdateJobEventPayload payloadObj =
                        objectMapper.readValue(payloadJson, StateUpdateJobEventPayload.class);

                // 2) build a StateUpdateJobEvent => type=UPDATE_FINDING
                StateUpdateJobEvent event = new StateUpdateJobEvent(payloadObj);
                // override the eventId with the job's ID
                event.setEventId(jobId);

                // 3) produce JSON to "bgJobsTopic"
                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(bgJobsTopic, eventJson);

            }
            else if (jobCategory == JobCategory.CREATE_TICKET){
                CreateTicketEventPayload payloadObj = objectMapper.readValue(payloadJson, CreateTicketEventPayload.class);

                CreateTicketEvent event = new CreateTicketEvent(payloadObj);
                event.setEventId(jobId);

                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(bgJobsTopic, eventJson);
            }

            else if (jobCategory == JobCategory.UPDATE_TICKET){
                UpdateTicketEventPayload payloadObj = objectMapper.readValue(payloadJson, UpdateTicketEventPayload.class);

                UpdateTicketEvent event = new UpdateTicketEvent(payloadObj);
                event.setEventId(jobId);

                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(bgJobsTopic, eventJson);
            }
            else if(jobCategory == JobCategory.RUNBOOK_JOB){
                // parse the payload as a RunbookJobEventPayload
                RunbookJobEventPayload payloadObj =
                    objectMapper.readValue(payloadJson, RunbookJobEventPayload.class);

                // build the event with type=RUNBOOK_JOB
                RunbookJobEvent event = new RunbookJobEvent(payloadObj);
                event.setEventId(jobId);

                // produce to bgJobs
                String eventJson = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(bgJobsTopic, eventJson);
            }
            
            else {
                System.err.println("[JfcProducerService] Unknown jobCategory => " 
                    + jobCategory + ". Not publishing anything.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
