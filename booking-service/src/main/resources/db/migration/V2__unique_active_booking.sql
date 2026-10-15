CREATE UNIQUE INDEX uq_bookings_active_event_user
    ON bookings (event_id, user_id)
    WHERE status <> 'CANCELLED';