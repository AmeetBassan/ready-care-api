CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_type AS ENUM ('CLIENT', 'PROFESSIONAL', 'ADMIN');
CREATE TYPE gender_type AS ENUM ('MALE', 'FEMALE');
CREATE TYPE verification_status AS ENUM ('NOT_SUBMITTED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED');
CREATE TYPE availability_slot_status AS ENUM ('FREE', 'BOOKED', 'BLOCKED');
CREATE TYPE booking_status AS ENUM ('REQUESTED', 'CONFIRMED', 'REJECTED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
CREATE TYPE payment_status AS ENUM ('UNPAID', 'PAID', 'REFUNDED', 'PARTIALLY_REFUNDED', 'FAILED');
CREATE TYPE currency_code AS ENUM ('GBP');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type user_type NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    gender gender_type NOT NULL,
    dob DATE NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone_number TEXT,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    primary_address_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label TEXT,
    line1 TEXT NOT NULL,
    line2 TEXT,
    city TEXT NOT NULL,
    postcode TEXT NOT NULL,
    country TEXT
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_primary_address
    FOREIGN KEY (primary_address_id) REFERENCES addresses(id) ON DELETE SET NULL;

CREATE TABLE client_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE professional_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    overall_verification_status verification_status NOT NULL DEFAULT 'NOT_SUBMITTED',
    bio TEXT,
    years_experience INTEGER,
    hourly_rate_office_hours NUMERIC(10,2),
    hourly_rate_out_of_office_hours NUMERIC(10,2)
);

CREATE TABLE admin_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE document_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    has_expiry BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE professional_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type_id UUID NOT NULL REFERENCES document_types(id),
    file_storage_key TEXT NOT NULL,
    status verification_status NOT NULL,
    expiry_date DATE,
    reviewed_by UUID REFERENCES admin_profiles(user_id),
    reviewed_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE availability_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_ts TIMESTAMPTZ NOT NULL,
    status availability_slot_status NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (professional_id, start_ts),
    CONSTRAINT availability_slots_on_hour CHECK (
        date_part('minute', start_ts) = 0 AND date_part('second', start_ts) = 0
    )
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_id UUID NOT NULL REFERENCES addresses(id) ON DELETE CASCADE,
    start_ts TIMESTAMPTZ NOT NULL,
    end_ts TIMESTAMPTZ NOT NULL,
    status booking_status NOT NULL,
    price NUMERIC(10,2),
    currency currency_code NOT NULL DEFAULT 'GBP',
    client_notes TEXT,
    professional_notes TEXT,
    cancelled_by user_type,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT booking_time_valid CHECK (end_ts > start_ts),
    CONSTRAINT booking_start_on_hour CHECK (date_part('minute', start_ts) = 0 AND date_part('second', start_ts) = 0),
    CONSTRAINT booking_end_on_hour CHECK (date_part('minute', end_ts) = 0 AND date_part('second', end_ts) = 0),
    CONSTRAINT booking_duration_hourly CHECK (extract(epoch from (end_ts - start_ts)) % 3600 = 0)
);

CREATE TABLE booking_slots (
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    availability_slot_id UUID NOT NULL REFERENCES availability_slots(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (booking_id, availability_slot_id),
    UNIQUE (availability_slot_id)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    status payment_status NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    paid_at TIMESTAMPTZ,
    provider TEXT,
    provider_reference TEXT,
    refunded_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    refunded_at TIMESTAMPTZ,
    refund_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reviews_rating_valid CHECK (rating BETWEEN 1 AND 5),
    UNIQUE (booking_id, reviewer_id)
);

INSERT INTO document_types (id, name, description, is_required, has_expiry)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Passport', 'Government issued passport', TRUE, TRUE),
    ('22222222-2222-2222-2222-222222222222', 'DBS Check', 'Background DBS certificate', TRUE, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Care Certificate', 'Care skills certification', TRUE, FALSE),
    ('44444444-4444-4444-4444-444444444444', 'First Aid', 'First aid certificate', FALSE, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, type, first_name, last_name, gender, dob, email, phone_number, password_hash)
VALUES (
    gen_random_uuid(),
    'ADMIN',
    'System',
    'Admin',
    'MALE',
    '1990-01-01',
    'admin@readycare.local',
    '0000000000',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO admin_profiles (user_id)
SELECT id
FROM users
WHERE email = 'admin@readycare.local'
ON CONFLICT (user_id) DO NOTHING;
