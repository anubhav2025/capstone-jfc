package com.capstone.jfc.dto.event;

import com.capstone.jfc.dto.event.payload.ScanRequestEventPayload;
import com.capstone.jfc.enums.EventTypes;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class ScanRequestEvent implements Event<ScanRequestEventPayload> {

    public static final EventTypes TYPE = EventTypes.SCAN_REQUEST;

    private String eventId;
    private ScanRequestEventPayload payload;

    public ScanRequestEvent() {}

    public ScanRequestEvent(ScanRequestEventPayload payload) {
        this.payload = payload;
        this.eventId = UUID.randomUUID().toString();
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public EventTypes getType() {
        return TYPE;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public ScanRequestEventPayload getPayload() {
        return payload;
    }
}
