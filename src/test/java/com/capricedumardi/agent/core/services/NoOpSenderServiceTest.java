package com.capricedumardi.agent.core.services;

import com.capricedumardi.agent.core.model.SendableRequestDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoOpSenderServiceTest {

    @Test
    void send_returnsFalse() {
        NoOpSenderService service = new NoOpSenderService("test reason");
        SendableRequestDto payload = new SendableRequestDto() {};
        assertFalse(service.send(payload));
    }

    @Test
    void close_doesNotThrow() {
        NoOpSenderService service = new NoOpSenderService("test reason");
        assertDoesNotThrow(service::close);
    }

    @Test
    void close_isIdempotent() {
        NoOpSenderService service = new NoOpSenderService("test reason");
        service.close();
        assertDoesNotThrow(service::close);
    }

    @Test
    void getDescription_returnsExpected() {
        NoOpSenderService service = new NoOpSenderService("test");
        assertEquals("No-OP Sender Service", service.getDescription());
    }
}
