package com.rubenmarin.bookingservice.infrastructure.persistence;

import com.rubenmarin.bookingservice.domain.model.*;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.InMemoryBookingRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

class InMemoryBookingRepositoryTest {

    private InMemoryBookingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
    }


    @Test
    void shouldReturnTrueWhenPendingBookingExists() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        repository.save(booking);

        Assertions.assertTrue(repository.existsActiveBooking(eventId, userId));
    }

    @Test
    void shouldReturnTrueWhenConfirmedBookingExists() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        Booking confirmed = booking.confirm();

        repository.save(confirmed);

        Assertions.assertTrue(repository.existsActiveBooking(eventId, userId));
    }

    @Test
    void shouldReturnFalseWhenBookingIsCancelled() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        repository.save(booking);

        Booking cancelled = booking.cancel();

        repository.save(cancelled);

        Assertions.assertFalse(repository.existsActiveBooking(eventId, userId));
    }

    @Test
    void shouldSaveAndFindBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking saved = repository.save(booking);

        Optional<Booking> result = repository.findById(booking.getBookingId());

        Assertions.assertTrue(result.isPresent());

        Booking found = result.get();

        Assertions.assertAll(
                () -> Assertions.assertSame(booking, saved),
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), found.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );
    }

    @Test
    void shouldReturnEmptyWhenBookingNotFound() {

        BookingId bookingId = new BookingId(UUID.randomUUID());

        Optional<Booking> result = repository.findById(bookingId);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldReplaceExistingBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking saved = repository.save(booking);
        Booking confirmed = saved.confirm();
        Booking savedAgain = repository.save(confirmed);

        Optional<Booking> result = repository.findById(booking.getBookingId());
        Assertions.assertTrue(result.isPresent());

        Booking found = result.get();
        Assertions.assertAll(
                // Verify the same booking identity is preserved
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), found.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), found.getUserId()),

                // Verify the repository returns the newly saved instance
                () -> Assertions.assertSame(confirmed, savedAgain),
                () -> Assertions.assertSame(savedAgain, found),

                // Verify the original booking remains unchanged
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, saved.getStatus()),

                // Verify the updated state is persisted
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, found.getStatus()),

                // Verify immutability
                () -> Assertions.assertNotSame(booking, confirmed)
        );
    }


    @Test
    void shouldReturnFalseWhenNoBookingExists() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        boolean exists = repository.existsActiveBooking(eventId, userId);

        Assertions.assertFalse(exists);
    }

    @Test
    void shouldReturnFalseWhenEventIdIsDifferent() {

        EventId eventId = new EventId(UUID.randomUUID());
        EventId anotherEventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        repository.save(new Booking(eventId, userId));

        boolean exists = repository.existsActiveBooking(anotherEventId, userId);

        Assertions.assertFalse(exists);
    }

    @Test
    void shouldReturnFalseWhenUserIdIsDifferent() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        UserId anotherUserId = new UserId(UUID.randomUUID());

        repository.save(new Booking(eventId, userId));

        boolean exists = repository.existsActiveBooking(eventId, anotherUserId);

        Assertions.assertFalse(exists);
    }
}