package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventAvailabilityAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class EventClientConfiguration {

    @Bean("eventRestClient")
    public RestClient eventRestClient(
            RestClient.Builder builder,
            @Value("${clients.event-service.base-url}") String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public EventAvailabilityPort eventAvailabilityPort(@Qualifier("eventRestClient") RestClient restClient) {
        return new RestEventAvailabilityAdapter(restClient);
    }

}
