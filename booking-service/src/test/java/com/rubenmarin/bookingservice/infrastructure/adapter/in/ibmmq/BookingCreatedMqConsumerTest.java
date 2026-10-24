
package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.port.in.ProcessBookingCreatedUseCase;
import jakarta.jms.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BookingCreatedMqConsumerTest {

    @Mock
    private ProcessBookingCreatedUseCase useCase;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message message;

    @InjectMocks
    private BookingCreatedMqConsumer consumer;

    @Test
    void shouldProcessBookingCreatedEvent() {
        BookingCreatedIntegrationEvent event = newEvent();
        String payload = "valid-json";

        Mockito.when(objectMapper.readValue(
                payload, BookingCreatedIntegrationEvent.class
        )).thenReturn(event);

        Assertions.assertDoesNotThrow(() -> consumer.consume(payload, message));
        Mockito.verify(useCase).process(event);
    }

    @Test
    void shouldIgnoreDuplicateInboxEvent() {
        BookingCreatedIntegrationEvent event = newEvent();
        String payload = "duplicate-json";

        Mockito.when(objectMapper.readValue(
                payload, BookingCreatedIntegrationEvent.class
        )).thenReturn(event);

        Mockito.doThrow(databaseException("23505", "inbox_events_pkey"))
                .when(useCase).process(event);

        DataIntegrityViolationException exception =
                databaseException("23505", "inbox_events_pkey");

        PSQLException cause = (PSQLException) exception.getCause();

        Assertions.assertEquals("23505", cause.getSQLState());
        Assertions.assertEquals("inbox_events_pkey",
                cause.getServerErrorMessage().getConstraint());

        Assertions.assertDoesNotThrow(() -> consumer.consume(payload, message));

        Mockito.verify(useCase).process(event);
    }

    @Test
    void shouldPropagateOtherConstraintViolation() {
        BookingCreatedIntegrationEvent event = newEvent();
        String payload = "invalid-json";

        Mockito.when(objectMapper.readValue(
                payload, BookingCreatedIntegrationEvent.class
        )).thenReturn(event);

        Mockito.doThrow(databaseException("23505", "other_table_pkey"))
                .when(useCase).process(event);

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> consumer.consume(payload, message)
        );

        Mockito.verify(useCase).process(event);
    }

    private BookingCreatedIntegrationEvent newEvent() {
        return new BookingCreatedIntegrationEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }


    private DataIntegrityViolationException databaseException(
            String sqlState,
            String constraint
    ) {
        PSQLException postgresException = Mockito.mock(PSQLException.class);
        ServerErrorMessage serverError = Mockito.mock(ServerErrorMessage.class);

        Mockito.when(postgresException.getSQLState()).thenReturn(sqlState);
        Mockito.when(postgresException.getServerErrorMessage()).thenReturn(serverError);
        Mockito.when(serverError.getConstraint()).thenReturn(constraint);

        return new DataIntegrityViolationException(
                "Database constraint violation",
                postgresException
        );
    }

}
