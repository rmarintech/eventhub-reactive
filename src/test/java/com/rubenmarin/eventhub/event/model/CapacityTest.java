package com.rubenmarin.eventhub.event.model;

import com.rubenmarin.eventhub.event.domain.model.Capacity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class CapacityTest {

    @Test
    void shouldCreateCapacityWithAllPlacesAvailable() {

        Capacity capacity = Capacity.of(20);

        Assertions.assertEquals(20, capacity.total());
        Assertions.assertEquals(20, capacity.available());
    }

    @Test
    void shouldRejectZeroTotalCapacity() {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Capacity.of(0)
        );
    }

    @Test
    void shouldRejectNegativeAvailableCapacity() {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Capacity(20, -1)
        );
    }

    @Test
    void shouldRejectAvailableCapacityGreaterThanTotal() {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Capacity(20, 21)
        );
    }

    @Test
    void shouldReservePlaces() {

        Capacity capacity = Capacity.of(20);

        Capacity updated = capacity.reserve(3);

        Assertions.assertEquals(20, updated.total());
        Assertions.assertEquals(17, updated.available());
    }

    @Test
    //significa nuestro Value Object inmutable.
    void shouldNotModifyOriginalCapacityWhenReserving() {

        Capacity original = Capacity.of(20);

        Capacity updated = original.reserve(3);

        Assertions.assertEquals(20, original.available());
        Assertions.assertEquals(17, updated.available());
    }

    @Test
    void shouldRejectReservationWhenNotEnoughPlacesAreAvailable() {

        Capacity capacity = new Capacity(20, 2);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> capacity.reserve(3)
        );
    }

    @Test
    void shouldReleasePlaces() {

        Capacity capacity = new Capacity(20, 15);

        Capacity updated = capacity.release(3);

        Assertions.assertEquals(18, updated.available());
    }

    @Test
    void shouldRejectReleaseBeyondTotalCapacity() {

        Capacity capacity = new Capacity(20, 19);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> capacity.release(2)
        );
    }
}