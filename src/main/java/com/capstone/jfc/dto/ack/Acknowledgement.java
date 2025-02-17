package com.capstone.jfc.dto.ack;

public interface Acknowledgement<T> {
    String getAcknowledgementId();
    T getPayload();
}
