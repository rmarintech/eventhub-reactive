package com.rubenmarin.bookingservice.application.service;

import com.rubenmarin.bookingservice.application.exception.BookingNotFoundException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository repository;

    @InjectMocks
    BookingService service;

    @Test
    void shouldCreateBooking() {

        // Mock repository to return the same Booking it receives
        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        Booking created = service.createBooking(eventId, userId);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        Mockito.verify(repository, Mockito.times(1))
                .save(captor.capture());

        Booking saved = captor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertNotNull(created.getBookingId()),
                () -> Assertions.assertEquals(eventId, created.getEventId()),
                () -> Assertions.assertEquals(userId, created.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, created.getStatus()),
                () -> Assertions.assertEquals(created.getBookingId(), saved.getBookingId()),

                () -> Assertions.assertSame(saved, created)
        );
    }

    @Test
    void shouldReturnBookingWhenExists() {
        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        BookingId bookingId = booking.getBookingId();
        Mockito.when(repository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        Booking found = service.getBooking(bookingId);
        Assertions.assertAll(
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), found.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus()),

                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),

                () -> Assertions.assertSame(found, booking));

        Mockito.verify(repository, Mockito.times(1)).findById(bookingId);
    }

    @Test
    void shouldThrowWhenBookingNotFound() {

        BookingId bookingId = new BookingId(UUID.randomUUID());

        Mockito.when(repository.findById(bookingId))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(BookingNotFoundException.class, () -> service.getBooking(bookingId));

        Mockito.verify(repository, Mockito.times(1)).findById(bookingId);
    }


    @Test
    void shouldConfirmBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        BookingId bookingId = booking.getBookingId();

        Mockito.when(repository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking confirmed = service.confirmBooking(bookingId);

        ArgumentCaptor<Booking> captor =
                ArgumentCaptor.forClass(Booking.class);

        Mockito.verify(repository).findById(bookingId);
        Mockito.verify(repository).save(captor.capture());

        Booking saved = captor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertEquals(bookingId, confirmed.getBookingId()),
                () -> Assertions.assertNotSame(booking, confirmed),
                () -> Assertions.assertSame(saved, confirmed)
        );
    }

    @Test
    void shouldRejectConfirmationOfCancelledBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking cancelled = booking.cancel();
        BookingId bookingId = cancelled.getBookingId();

        Mockito.when(repository.findById(bookingId))
                .thenReturn(Optional.of(cancelled));

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.confirmBooking(bookingId)
        );

        Mockito.verify(repository).findById(bookingId);
        Mockito.verify(repository, Mockito.never())
                .save(Mockito.any(Booking.class));

        Assertions.assertEquals(
                BookingStatus.CANCELLED,
                cancelled.getStatus()
        );
    }

    @Test
    void shouldCancelBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking confirmed = booking.confirm();
        BookingId bookingId = confirmed.getBookingId();

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.of(confirmed));

        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = service.cancelBooking(bookingId);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        Mockito.verify(repository).findById(bookingId);
        Mockito.verify(repository).save(captor.capture());

        Booking saved = captor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertEquals(bookingId, cancelled.getBookingId()),
                () -> Assertions.assertNotSame(confirmed, cancelled),
                () -> Assertions.assertSame(saved, cancelled)
        );
    }

    @Test
    void shouldRejectCancellationOfCancelledBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking cancelled = booking.cancel();
        BookingId bookingId = cancelled.getBookingId();

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.of(cancelled));

        Assertions.assertThrows(IllegalStateException.class, () -> service.cancelBooking(bookingId));

        Mockito.verify(repository).findById(bookingId);
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void shouldThrowWhenConfirmingNonExistingBooking() {

        BookingId bookingId = new BookingId(UUID.randomUUID());

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.empty());

        Assertions.assertThrows(BookingNotFoundException.class, () -> service.confirmBooking(bookingId));

        Mockito.verify(repository).findById(bookingId);
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));
    }
}