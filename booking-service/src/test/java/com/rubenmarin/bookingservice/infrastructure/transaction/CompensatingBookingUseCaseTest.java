package com.rubenmarin.bookingservice.infrastructure.transaction;

import com.rubenmarin.bookingservice.application.exception.EventNotAvailableException;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

class CompensatingBookingUseCaseTest {

    private BookingUseCase transactionalDelegate;
    private EventCapacityPort eventCapacityPort;
    private CompensatingBookingUseCase compensatingUseCase;

    @BeforeEach
    void setUp() {
        transactionalDelegate = Mockito.mock(BookingUseCase.class);
        eventCapacityPort = Mockito.mock(EventCapacityPort.class);

        compensatingUseCase = new CompensatingBookingUseCase(
                transactionalDelegate,
                eventCapacityPort
        );
    }

    @Test
    void shouldCreateBookingWithoutCompensationWhenSuccessful() {

//        reservePlaces()       OK
//        createBooking()        OK
//        releasePlaces()        NO

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking expectedBooking = new Booking(eventId, userId);

        Mockito.when(transactionalDelegate.createBooking(eventId, userId))
                .thenReturn(expectedBooking);

        Booking result = compensatingUseCase.createBooking(eventId, userId);

        Assertions.assertSame(expectedBooking, result);

        Mockito.verify(eventCapacityPort, Mockito.times(1)).reservePlaces(eventId, 1);
        Mockito.verify(transactionalDelegate, Mockito.times(1)).createBooking(eventId, userId);
        Mockito.verify(eventCapacityPort, Mockito.never()).releasePlaces(Mockito.any(EventId.class), Mockito.anyInt());
    }

    @Test
    void shouldReleasePlacesWhenBookingCreationFails() {

//        reservePlaces()       OK
//        createBooking()        ERROR
//        releasePlaces()        OK
//        Propagamos el error original

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        IllegalStateException failure = new IllegalStateException("Simulated booking creation failure");

        Mockito.when(transactionalDelegate.createBooking(eventId, userId))
                .thenThrow(failure);

        IllegalStateException thrown = Assertions.assertThrows(
                IllegalStateException.class,
                () -> compensatingUseCase.createBooking(eventId, userId)
        );

        Assertions.assertSame(failure, thrown);

        Mockito.verify(eventCapacityPort, Mockito.times(1)).reservePlaces(eventId, 1);
        Mockito.verify(transactionalDelegate, Mockito.times(1)).createBooking(eventId, userId);
        Mockito.verify(eventCapacityPort, Mockito.times(1)).releasePlaces(eventId, 1);
    }

    @Test
    void shouldNotCreateBookingOrCompensateWhenReservationFails() {

//        reservePlaces()       ERROR
//        createBooking()        NO
//        releasePlaces()        NO
//        Propagamos el error de reserva

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        EventNotAvailableException failure = new EventNotAvailableException(eventId);

        Mockito.doThrow(failure)
                .when(eventCapacityPort)
                .reservePlaces(eventId, 1);

        EventNotAvailableException thrown = Assertions.assertThrows(
                EventNotAvailableException.class,
                () -> compensatingUseCase.createBooking(eventId, userId)
        );

        Assertions.assertSame(failure, thrown);

        Mockito.verify(eventCapacityPort, Mockito.times(1)).reservePlaces(eventId, 1);
        Mockito.verifyNoInteractions(transactionalDelegate);
        Mockito.verify(eventCapacityPort, Mockito.never()).releasePlaces(Mockito.any(EventId.class), Mockito.anyInt());
    }
}