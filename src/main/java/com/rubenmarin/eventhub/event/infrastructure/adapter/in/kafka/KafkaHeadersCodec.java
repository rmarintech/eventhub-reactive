package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Component
public class KafkaHeadersCodec {

    private final ObjectMapper mapper;

    public KafkaHeadersCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String serialize(Headers headers) {

        Objects.requireNonNull(headers, "Headers cannot be null");

        List<HeaderEntry> entries = new ArrayList<>();

        for (Header header : headers) {
            entries.add(new HeaderEntry(
                    header.key(),
                    header.value() == null
                            ? null
                            : Base64.getEncoder().encodeToString(header.value())
            ));
        }

        return mapper.writeValueAsString(entries);
    }

    public Headers deserialize(String json) {

        Objects.requireNonNull(json, "Headers JSON cannot be null");

        HeaderEntry[] entries = mapper.readValue(json, HeaderEntry[].class);

        Headers headers = new RecordHeaders();

        for (HeaderEntry entry : entries) {
            byte[] value = entry.value() == null
                    ? null
                    : Base64.getDecoder().decode(entry.value());

            headers.add(entry.key(), value);
        }

        return headers;
    }

    private record HeaderEntry(
            String key,
            String value
    ) {}
}