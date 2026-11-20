package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool;

import com.rubenmarin.aiassistantservice.application.exception.EventNotFoundException;
import com.rubenmarin.aiassistantservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.exception.InvalidToolArgumentException;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EventToolsTest {

    @Mock
    private GetAvailableSeatsUseCase getAvailableSeatsUseCase;

    private EventTools eventTools;
    private SimpleMeterRegistry simpleMeterRegistry;

    @BeforeEach
    void setUp() {

        //SimpleMeterRegistry: almacena las métricas en memoria para poder consultarlas.
        simpleMeterRegistry = new SimpleMeterRegistry();

        // ObservationRegistry: gestiona las observaciones.
        ObservationRegistry observationRegistry = ObservationRegistry.create();

        observationRegistry.observationConfig()
                .observationHandler(
                        // DefaultMeterObservationHandler: escucha las observaciones y registra sus métricas.
                        new DefaultMeterObservationHandler(simpleMeterRegistry)
                );

        eventTools = new EventTools(
                getAvailableSeatsUseCase,
                observationRegistry
        );
    }

    @Test
    void shouldRecordSuccessObservation() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        Mockito.when(getAvailableSeatsUseCase.getAvailableSeats(eventId))
                .thenReturn(Mono.just(25));

        // WHEN
        int available = eventTools.getAvailableSeats(eventId.toString());

        // THEN
        Assertions.assertEquals(25, available);

        Timer timer = simpleMeterRegistry.get("eventhub.ai.get_available_seats")
                .tag("outcome", "success")
                .tag("error", "none")
                .timer();

        Assertions.assertEquals(1, timer.count());

        Mockito.verify(getAvailableSeatsUseCase).getAvailableSeats(eventId);
    }

    @Test
    void shouldRecordNotFoundObservation() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        EventNotFoundException expected = new EventNotFoundException(eventId);

        Mockito.when(getAvailableSeatsUseCase.getAvailableSeats(eventId))
                .thenReturn(Mono.error(expected));

        // WHEN & THEN
        EventNotFoundException actual =
                Assertions.assertThrows(EventNotFoundException.class, () -> eventTools.getAvailableSeats(eventId.toString()));

        Timer timer = simpleMeterRegistry.get("eventhub.ai.get_available_seats")
                .tag("outcome", "not_found")
                .tag("error", "EventNotFoundException")
                .timer();

        Assertions.assertEquals(1, timer.count());

        Mockito.verify(getAvailableSeatsUseCase).getAvailableSeats(eventId);

        Assertions.assertSame(expected, actual);
    }

    @Test
    void shouldRecordErrorObservation() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        EventServiceUnavailableException expected = new EventServiceUnavailableException();

        Mockito.when(getAvailableSeatsUseCase.getAvailableSeats(eventId))
                .thenReturn(Mono.error(expected));

        // WHEN & THEN
        EventServiceUnavailableException actual =
                Assertions.assertThrows(EventServiceUnavailableException.class, () -> eventTools.getAvailableSeats(eventId.toString()));

        Timer timer = simpleMeterRegistry.get("eventhub.ai.get_available_seats")
                .tag("outcome", "error")
                .tag("error", "EventServiceUnavailableException")
                .timer();

        Assertions.assertEquals(1, timer.count());

        Mockito.verify(getAvailableSeatsUseCase).getAvailableSeats(eventId);

        Assertions.assertSame(expected, actual);
    }

    @Test
    void shouldRejectInvalidUuid() {

        // GIVEN
        String invalidEventId = "invalid-uuid";

        // WHEN & THEN
        Assertions.assertThrows(
                InvalidToolArgumentException.class,
                () -> eventTools.getAvailableSeats(invalidEventId)
        );

        // The use case must never be invoked.
        Mockito.verifyNoInteractions(getAvailableSeatsUseCase);

        // UUID validation currently happens outside the observation.
        // Therefore, no availability metric should be registered.
        Assertions.assertNull(
                simpleMeterRegistry.find("eventhub.ai.get_available_seats")
                        .timer()
        );
    }
}
