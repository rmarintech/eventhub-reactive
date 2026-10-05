CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    capacity_total INTEGER NOT NULL,
    capacity_available INTEGER NOT NULL,
    price_amount NUMERIC(12, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL
);


CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    event_id UUID NOT NULL,
    places INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL
);