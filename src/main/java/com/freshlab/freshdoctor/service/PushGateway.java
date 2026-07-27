package com.freshlab.freshdoctor.service;

public interface PushGateway {
    PushSendResult send(String registrationKey, PushMessage message);
}
