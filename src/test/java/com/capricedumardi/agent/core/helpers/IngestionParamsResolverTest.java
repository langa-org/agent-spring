package com.capricedumardi.agent.core.helpers;

import com.capricedumardi.agent.core.model.SenderType;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class IngestionParamsResolverTest {

    private static final String ACCOUNT_KEY = "acct-001";
    private static final String APP_KEY = "app-001";
    private static final String SECRET = "mySecret";

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static String buildUrl(String base, String type, String credentials) {
        return base + "/api/ingestion/" + type + "/" + credentials;
    }

    @Test
    void constructor_rejectsNullUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver(null, SECRET));
    }

    @Test
    void constructor_rejectsEmptyUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver("  ", SECRET));
    }

    @Test
    void constructor_rejectsNullSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver("https://example.com/api/ingestion/h/creds", null));
    }

    @Test
    void constructor_rejectsEmptySecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver("https://example.com/api/ingestion/h/creds", " "));
    }

    @Test
    void constructor_rejectsUrlWithoutIngestionEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new IngestionParamsResolver("https://example.com/other", SECRET));
    }

    @Test
    void resolveHttpUrl_extractsBaseUrl() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals("https://api.langa.io/api/ingestion", resolver.resolveHttpUrl());
    }

    @Test
    void resolveSenderType_http() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals(SenderType.HTTP, resolver.resolveSenderType());
    }

    @Test
    void resolveSenderType_kafka() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = "kafka://broker:9092/api/ingestion/ktopic/" + encoded;

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals(SenderType.KAFKA, resolver.resolveSenderType());
    }

    @Test
    void resolveAppKey_extractsCorrectly() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals(APP_KEY, resolver.resolveAppKey());
    }

    @Test
    void resolveAccountKey_extractsCorrectly() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals(ACCOUNT_KEY, resolver.resolveAccountKey());
    }

    @Test
    void resolveSecret_returnsProvidedSecret() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals(SECRET, resolver.resolveSecret());
    }

    @Test
    void resolveBootstrapServer_extractsKafkaServer() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = "kafka://broker:9092/api/ingestion/mytopic/" + encoded;

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals("broker:9092", resolver.resolveBootStrapServer());
    }

    @Test
    void resolveTopic_extractsKafkaTopic() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = "kafka://broker:9092/api/ingestion/mytopic/" + encoded;

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertEquals("mytopic", resolver.resolveTopic());
    }

    @Test
    void resolveAppKey_invalidBase64_throwsException() {
        String url = "https://api.langa.io/api/ingestion/h/not-valid-base64!!!";

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertThrows(IllegalStateException.class, resolver::resolveAppKey);
    }

    @Test
    void resolveAccountKey_missingDelimiter_throwsException() {
        String encoded = encode("nolga_delimiter");
        String url = buildUrl("https://api.langa.io", "h", encoded);

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, SECRET);
        assertThrows(IllegalStateException.class, resolver::resolveAccountKey);
    }

    @Test
    void constructor_trimsUrlAndSecret() {
        String encoded = encode(ACCOUNT_KEY + "-lga-" + APP_KEY);
        String url = "  " + buildUrl("https://api.langa.io", "h", encoded) + "  ";

        IngestionParamsResolver resolver = new IngestionParamsResolver(url, "  " + SECRET + "  ");
        assertEquals(APP_KEY, resolver.resolveAppKey());
        assertEquals(SECRET, resolver.resolveSecret());
    }
}
