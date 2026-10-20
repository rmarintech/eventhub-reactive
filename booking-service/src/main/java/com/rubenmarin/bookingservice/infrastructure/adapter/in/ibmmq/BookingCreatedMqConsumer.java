package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.port.in.ProcessBookingCreatedUseCase;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;

@Component
public class BookingCreatedMqConsumer {

    private static final Logger _log = LoggerFactory.getLogger(BookingCreatedMqConsumer.class);

    private final ProcessBookingCreatedUseCase processBookingCreatedUseCase;
    private final ObjectMapper objectMapper;

    public BookingCreatedMqConsumer(
            ProcessBookingCreatedUseCase processBookingCreatedUseCase,
            ObjectMapper objectMapper) {
        this.processBookingCreatedUseCase = processBookingCreatedUseCase;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "BOOKING.CREATED.Q")
    public void consume(String payload, Message message) {

        _log.info("******************************************");
        _log.info("Received Booking Created Event to MQ: {}", payload);

        try {
            _log.info("JMSDeliveryMode: {}", message.getJMSDeliveryMode());
        } catch (JMSException e) {
            _log.error(e.getMessage());
        }

        BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent =
                objectMapper.readValue(payload, BookingCreatedIntegrationEvent.class);

        try {

            processBookingCreatedUseCase.process(bookingCreatedIntegrationEvent);

        } catch (DataIntegrityViolationException e) {
            // Si dos consumidores procesan simultáneamente el mismo evento,
            // PostgreSQL puede rechazar uno por clave duplicada (SQLSTATE 23505).
            // Comprobamos que el error corresponde a la PK del Inbox (inbox_events_pkey).
            // Si es un duplicado, lo ignoramos para confirmar el consumo JMS.
            // Cualquier otro error se propaga para provocar el rollback.
            Throwable cause = e;

            while (cause != null) {
                if (cause instanceof org.postgresql.util.PSQLException sqlException
                        && "23505".equals(sqlException.getSQLState())
                        && sqlException.getServerErrorMessage() != null
                        && "inbox_events_pkey".equals(
                        sqlException.getServerErrorMessage().getConstraint())) {

                    _log.info("Duplicate Inbox event ignored");
                    return;
                }

                cause = cause.getCause();
            }

            throw e;
        }
    }
}
