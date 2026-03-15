package com.capricedumardi.agent.core.config.actuator;

import com.capricedumardi.agent.core.config.jmx.AgentManagement;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LangaControlEndpointTest {

    @Test
    void getConfig_returnsExpectedKeys() {
        // AgentManagement.getInstance() initializes from ConfigLoader
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        Map<String, Object> config = endpoint.getConfig();

        assertNotNull(config);
        assertTrue(config.containsKey("batchSize"));
        assertTrue(config.containsKey("flushIntervalSeconds"));
        assertTrue(config.containsKey("debugMode"));
        assertTrue(config.containsKey("compressionThreshold"));
        assertTrue(config.containsKey("agentVersion"));
        assertTrue(config.containsKey("ingestionUrl"));
        assertTrue(config.containsKey("loggingFramework"));
    }

    @Test
    void updateConfig_batchSize_isApplied() {
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        AgentManagement management = AgentManagement.getInstance();

        int original = management.getBufferBatchSize();
        try {
            endpoint.updateConfig(999, null, null, null);
            assertEquals(999, management.getBufferBatchSize());
        } finally {
            management.setBufferBatchSize(original);
        }
    }

    @Test
    void updateConfig_flushInterval_positiveOnly() {
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        AgentManagement management = AgentManagement.getInstance();

        int original = management.getBufferFlushIntervalSeconds();
        try {
            endpoint.updateConfig(null, 15, null, null);
            assertEquals(15, management.getBufferFlushIntervalSeconds());

            // Zero or negative should not change the value
            endpoint.updateConfig(null, 0, null, null);
            assertEquals(15, management.getBufferFlushIntervalSeconds());
        } finally {
            management.setBufferFlushIntervalSeconds(original);
        }
    }

    @Test
    void updateConfig_debugMode_isApplied() {
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        AgentManagement management = AgentManagement.getInstance();

        boolean original = management.isDebugMode();
        try {
            endpoint.updateConfig(null, null, true, null);
            assertTrue(management.isDebugMode());

            endpoint.updateConfig(null, null, false, null);
            assertFalse(management.isDebugMode());
        } finally {
            management.setDebugMode(original);
        }
    }

    @Test
    void updateConfig_compressionThreshold_positiveOnly() {
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        AgentManagement management = AgentManagement.getInstance();

        int original = management.getHttpCompressionThresholdBytes();
        try {
            endpoint.updateConfig(null, null, null, 4096);
            assertEquals(4096, management.getHttpCompressionThresholdBytes());

            // Zero should not change
            endpoint.updateConfig(null, null, null, 0);
            assertEquals(4096, management.getHttpCompressionThresholdBytes());
        } finally {
            management.setHttpCompressionThresholdBytes(original);
        }
    }

    @Test
    void updateConfig_nullParams_noChange() {
        LangaControlEndpoint endpoint = new LangaControlEndpoint();
        AgentManagement management = AgentManagement.getInstance();

        int batchBefore = management.getBufferBatchSize();
        int flushBefore = management.getBufferFlushIntervalSeconds();

        endpoint.updateConfig(null, null, null, null);

        assertEquals(batchBefore, management.getBufferBatchSize());
        assertEquals(flushBefore, management.getBufferFlushIntervalSeconds());
    }
}
