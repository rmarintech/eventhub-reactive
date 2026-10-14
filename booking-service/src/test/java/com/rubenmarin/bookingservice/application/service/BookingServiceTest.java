
package com.rubenmarin.bookingservice.application.service;

import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.exception.BookingNotFoundException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
    BookingService bookingService;

    @Test
    void shouldCreateBookingWhenNoActiveBookingExists() {

        // Arrange
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Mockito.when(repository.existsActiveBooking(eventId, userId)).thenReturn(false);

        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking created = bookingService.createBooking(eventId, userId);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertNotNull(created.getBookingId()),
                () -> Assertions.assertEquals(eventId, created.getEventId()),
                () -> Assertions.assertEquals(userId, created.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, created.getStatus())
        );

        Mockito.verify(repository).existsActiveBooking(eventId, userId);
        Mockito.verify(repository).save(Mockito.same(created));
    }

    @Test
    void shouldThrowExceptionWhenActiveBookingAlreadyExists() {

        // Arrange
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Mockito.when(repository.existsActiveBooking(eventId, userId)).thenReturn(true);

        // Act + Assert
        Assertions.assertThrows(
                BookingAlreadyExistsException.class,
                () -> bookingService.createBooking(eventId, userId)
        );

        Mockito.verify(repository).existsActiveBooking(eventId, userId);

        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));
    }

    @Test
    void shouldReturnBookingWhenExists() {

        // Arrange
        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        BookingId bookingId = booking.getBookingId();

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        Booking found = bookingService.getBooking(bookingId);

        // Assert
        Assertions.assertSame(booking, found);

        Mockito.verify(repository).findById(bookingId);
    }

    @Test
    void shouldThrowExceptionWhenBookingDoesNotExist() {

        // Arrange
        BookingId bookingId = new BookingId(UUID.randomUUID());

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.getBooking(bookingId)
        );

        Mockito.verify(repository).findById(bookingId);

        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));
    }

    @Test
    void shouldConfirmPendingBooking() {

        // Arrange
        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Mockito.when(repository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));

        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking confirmed = bookingService.confirmBooking(booking.getBookingId());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(booking.getBookingId(), confirmed.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), confirmed.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), confirmed.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertNotSame(booking, confirmed)
        );

        Mockito.verify(repository).findById(booking.getBookingId());
        Mockito.verify(repository).save(Mockito.same(confirmed));
    }

    @Test
    void shouldCancelPendingBooking() {

        // Arrange
        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Mockito.when(repository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));

        Mockito.when(repository.save(Mockito.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking cancelled = bookingService.cancelBooking(booking.getBookingId());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(booking.getBookingId(), cancelled.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), cancelled.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), cancelled.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertNotSame(booking, cancelled)
        );

        Mockito.verify(repository).findById(booking.getBookingId());
        Mockito.verify(repository).save(Mockito.same(cancelled));
    }

    @Test
    void shouldThrowExceptionWhenConfirmingNonExistingBooking() {

        // Arrange
        BookingId bookingId = new BookingId(UUID.randomUUID());

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.confirmBooking(bookingId)
        );

        Mockito.verify(repository).findById(bookingId);

        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));
    }

    @Test
    void shouldThrowExceptionWhenCancellingNonExistingBooking() {

        // Arrange
        BookingId bookingId = new BookingId(UUID.randomUUID());

        Mockito.when(repository.findById(bookingId)).thenReturn(Optional.empty());

        // Act + Assert
        Assertions.assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancelBooking(bookingId)
        );

        Mockito.verify(repository).findById(bookingId);

        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Booking.class));
    }
}
