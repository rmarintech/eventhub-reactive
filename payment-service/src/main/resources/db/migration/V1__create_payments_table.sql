CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    payment_status VARCHAR(20) NOT NULL,
    booking_id UUID NOT NULL,
    payment_request_id UUID NOT NULL UNIQUE,
    money_amount NUMERIC(19,4) NOT NULL,
    money_currency VARCHAR(3) NOT NULL,

    CONSTRAINT chk_payment_amount_positive
      CHECK (money_amount > 0),

    CONSTRAINT chk_payment_status
      CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED'))
);