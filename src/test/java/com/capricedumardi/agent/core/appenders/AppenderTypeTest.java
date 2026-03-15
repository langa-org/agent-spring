package com.capricedumardi.agent.core.appenders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppenderTypeTest {

    @Test
    void values_containsBothTypes() {
        AppenderType[] values = AppenderType.values();
        assertEquals(2, values.length);
        assertNotNull(AppenderType.valueOf("LANGA_LOGBACK_APPENDER"));
        assertNotNull(AppenderType.valueOf("LANGA_LOG4J_APPENDER"));
    }
}
