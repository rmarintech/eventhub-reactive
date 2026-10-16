package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import com.rubenmarin.bookingservice.application.exception.EventNotAvailableException;
import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

public class RestEventCapacityAdapter implements EventCapacityPort {

    private final RestClient restClient;

    public RestEventCapacityAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void reservePlaces(EventId eventId, int places) {
        try {

            restClient
                    .post()
                    .uri("/events/{id}/reserve/{places}/places", eventId.eventId(), places)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new EventNotAvailableException(eventId);

        } catch (HttpClientErrorException.NotFound ex) {
            throw new EventNotFoundException(eventId, ex);

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new EventServiceUnavailableException(ex);
        }
    }

    @Override
    public void releasePlaces(EventId eventId, int places) {
        try {
            restClient
                    .post()
                    .uri("/events/{id}/release/{places}/places", eventId.eventId(), places)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new EventNotFoundException(eventId, ex);

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new EventServiceUnavailableException(ex);
        }

    }
}
