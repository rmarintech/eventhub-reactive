package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

public class RestEventAvailabilityAdapter implements EventAvailabilityPort {

    private final RestClient restClient;

    public RestEventAvailabilityAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean isAvailable(EventId eventId) {

        // GET /events/{id}
        // Deserializar EventResponse
        // Comprobar PUBLISHED y available > 0
        try {
            EventResponse response = restClient
                    .get()
                    .uri("/events/{id}", eventId.eventId())
                    .retrieve()
                    .body(EventResponse.class);
            // El métod0 retrieve() prepara la extracción de la respuesta; la llamada HTTP se realiza al consumirla con body().

            return response != null
                    && "PUBLISHED".equals(response.status())
                    && response.available() > 0;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new EventNotFoundException(eventId, ex);

        } catch (HttpServerErrorException | ResourceAccessException ex) {

//            Event Service devuelve HTTP 500--------HttpServerErrorException
//            Conexión rechazada---------------------ResourceAccessException
//            Timeout de conexión o lectura----------Habitualmente ResourceAccessException

            throw new EventServiceUnavailableException(ex);
        }
    }
}
