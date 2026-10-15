CREATE TABLE bookings (
                          booking_id UUID PRIMARY KEY,
                          event_id UUID NOT NULL,
                          user_id UUID NOT NULL,
                          status VARCHAR(20) NOT NULL
);