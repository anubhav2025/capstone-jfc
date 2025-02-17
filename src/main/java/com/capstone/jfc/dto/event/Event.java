package com.capstone.jfc.dto.event;

import com.capstone.jfc.enums.EventTypes;

public interface Event<T> {
    String getEventId();
    EventTypes getType();
    T getPayload();
}
