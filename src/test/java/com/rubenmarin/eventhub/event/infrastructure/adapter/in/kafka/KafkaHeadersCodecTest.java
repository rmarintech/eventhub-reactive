package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class KafkaHeadersCodecTest {

    private KafkaHeadersCodec codec;

    @BeforeEach
    void setUp() {
        codec = new KafkaHeadersCodec(new ObjectMapper());
    }

    @Test
    void shouldSerializeAndDeserializeHeaders() {

        Headers original = new RecordHeaders();

        original.add(
                "event-type",
                "BookingCreated".getBytes(StandardCharsets.UTF_8)
        );

        original.add(
                "correlation-id",
                "abc-123".getBytes(StandardCharsets.UTF_8)
        );

        String json = codec.serialize(original);

        Headers restored = codec.deserialize(json);

        assertArrayEquals(
                original.lastHeader("event-type").value(),
                restored.lastHeader("event-type").value()
        );

        assertArrayEquals(
                original.lastHeader("correlation-id").value(),
                restored.lastHeader("correlation-id").value()
        );
    }

    @Test
    void shouldPreserveBinaryHeaders() {

        Headers original = new RecordHeaders();

        byte[] binaryValue = {
                0x00,
                (byte) 0xFF,
                (byte) 0x80,
                0x01
        };

        original.add("binary-header", binaryValue);

        String json = codec.serialize(original);

        Headers restored = codec.deserialize(json);

        assertArrayEquals(
                binaryValue,
                restored.lastHeader("binary-header").value()
        );
    }

    @Test
    void shouldPreserveDuplicateHeaders() {

        Headers original = new RecordHeaders();

        original.add(
                "trace-id",
                "first".getBytes(StandardCharsets.UTF_8)
        );

        original.add(
                "trace-id",
                "second".getBytes(StandardCharsets.UTF_8)
        );

        String json = codec.serialize(original);

        Headers restored = codec.deserialize(json);

        var headers = restored.headers("trace-id");

        assertEquals(
                2,
                Arrays.stream(
                        java.util.stream.StreamSupport
                                .stream(headers.spliterator(), false)
                                .toArray()
                ).count()
        );
    }

    @Test
    void shouldPreserveNullHeaderValues() {

        Headers original = new RecordHeaders();

        original.add("nullable-header", null);

        String json = codec.serialize(original);

        Headers restored = codec.deserialize(json);

        assertNotNull(restored.lastHeader("nullable-header"));

        assertNull(restored.lastHeader("nullable-header").value());
    }

    @Test
    void shouldHandleEmptyHeaders() {

        Headers original = new RecordHeaders();

        String json = codec.serialize(original);

        Headers restored = codec.deserialize(json);

        assertFalse(restored.iterator().hasNext());
    }
}