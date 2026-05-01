# ReadyCare API (POC)

Simple Spring Boot backend for the ReadyCare POC scope:
- professional/client account lifecycle
- professional document upload and review
- availability slots
- professional search by city + timeslot
- booking lifecycle with mock payments
- scheduled auto-rejection/refund for unconfirmed bookings

## Tech
- Java 21
- Spring Boot 3.4
- PostgreSQL + Flyway migrations
- Azure Blob Storage

## What is implemented

### Accounts
- `POST /auth/register/professional` create professional + profile + primary address
- `POST /auth/register/client` create client + profile + primary address
- `POST /auth/login/professional` login professional and return JWT
- `POST /auth/login/client` login client and return JWT
- `GET /api/professionals` list all professionals
- `GET /api/professionals/{professionalId}` get one professional (includes `primaryAddressId`)
- `GET /api/professionals/{professionalId}/address` get professional primary address
- `PUT /api/professionals/{professionalId}` update professional/profile fields (including hourly rates)
- `DELETE /api/professionals/{professionalId}` hard-delete user + related db data + document objects
- `GET /api/clients/{clientId}` get one client (includes `primaryAddressId`)
- `PUT /api/clients/{clientId}` update client fields
- `DELETE /api/clients/{clientId}` hard-delete client + related db data

### Verification Documents
- `GET /document-types` list supported document types for upload
- `POST /document-types` admin-only create supported document type
- `PATCH /document-types/{documentTypeId}` admin-only update supported document type
- `POST /api/professionals/{professionalId}/documents` multipart upload (`documentTypeId`, `file`, optional `expiryDate`)
- `GET /api/professionals/{professionalId}/documents`
- `PATCH /api/admin/documents/{documentId}/review` approve/reject
- Professional `overall_verification_status` is recalculated automatically from required docs.

### Availability
- `PUT /api/professionals/{professionalId}/availability` upsert slots for a day (`startHour` to `endHour`)
- `GET /api/professionals/{professionalId}/availability?from=...&to=...`
- `PATCH /api/professionals/{professionalId}/availability/{slotId}?status=FREE|BOOKED|BLOCKED`

### Search
- `GET /api/professionals/search?city=London&startTs=2026-02-14T10:00:00Z&durationHours=2`
- Returns only professionals with `overall_verification_status=APPROVED` and enough `FREE` slots.

### Bookings + Payments
- `POST /api/bookings` creates booking and reserves slots
- Payment is mocked automatically as `PAID`
- `GET /api/bookings/client/{clientId}`
- `GET /api/bookings/professional/{professionalId}`
- `POST /api/bookings/{bookingId}/confirm?professionalId=...`
- `POST /api/bookings/{bookingId}/reject?professionalId=...` (body has reason)
- `POST /api/bookings/{bookingId}/cancel` (actor + cancelledBy)
  - client cancellation within 48h: no refund
  - outside 48h: full refund
- `POST /api/bookings/{bookingId}/no-show?clientId=...`
- `POST /api/bookings/{bookingId}/complete?professionalId=...` only after booking end time

Note:
- Booking `addressId` must be the **client** address ID (not the professional address).
- Client and professional responses include `primaryAddressId` for convenience.

### Scheduler
- Every 60s, requested bookings with `start_ts < now` are auto-rejected with reason `NOT_ACCEPTED_IN_TIME` and fully refunded.

## Run locally

### 1) Start PostgreSQL
```bash
docker compose up -d
```

### 2) Run app
```bash
mvn spring-boot:run
```

App runs at `http://localhost:8080`.

## Config
Configuration lives in `src/main/resources/application.yml`.

### Database
The app uses these environment variables if present, otherwise it falls back to the local Docker database:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/readycare'
export DB_USERNAME='readycare'
export DB_PASSWORD='readycare'
```

### JWT
Set a JWT secret for any shared/dev/prod environment so issued tokens and token validation use the same signing key:

```bash
export JWT_SECRET='replace-with-a-long-random-secret'
```

Optional:

Edit `app.jwt.expiration` in `application.yml`.

### Document Storage
Uploaded files are stored in Azure Blob Storage.
- Professional verification documents use the `documents` container.
- User profile pictures use the `profilepics` container.

`file_storage_key` stores the blob key, for example:

```text
professionals/<professional-id>/documents/<uuid>-passport.pdf
```

Set the Azure Storage connection string before running the app:

```bash
export AZURE_STORAGE_CONNECTION_STRING='DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net'
```

Container names are configured in `application.yml`:
- `app.storage.documents-container-name: documents`
- `app.storage.profile-pictures-container-name: profilepics`

### Optional Runtime Settings
These have defaults and only need changing if required:

```bash
export APP_CANCELLATION_FULL_REFUND_HOURS_THRESHOLD=48
export APP_BOOKING_TIMEOUT_FIXED_DELAY_MS=60000
export APP_BOOKING_REMINDER_FIXED_DELAY_MS=60000
export APP_BOOKING_ALLOW_EARLY_COMPLETE=false
```

`APP_BOOKING_ALLOW_EARLY_COMPLETE=true` lets professionals mark a confirmed booking as completed even before end time (useful for dev/testing only).

### Email Notifications
Email notifications are integrated for:
1. account registration confirmation
2. booking created (client + professional)
3. booking cancelled (client + professional)
4. booking reminder ~1 hour before start (client + professional, once per booking)

Configure:

```bash
export AZURE_COMMUNICATION_SERVICES_CONNECTION_STRING='endpoint=https://...;accesskey=...'
export APP_EMAIL_SENDER_ADDRESS='DoNotReply@<your-domain>'
```

## Seeded admin user
Flyway seeds one admin user for document review:
- email: `admin@readycare.local`
- password (plain): `admin123`
- stored hash: SHA-256

## Example payloads

### Create professional
```json
{
  "firstName": "Sarah",
  "lastName": "Khan",
  "gender": "FEMALE",
  "dob": "1992-04-03",
  "email": "sarah@example.com",
  "phoneNumber": "447700000000",
  "password": "test123",
  "bio": "Experienced elderly care worker",
  "yearsExperience": 5,
  "hourlyRateOfficeHours": 25.00,
  "hourlyRateOutOfOfficeHours": 30.00,
  "primaryAddress": {
    "line1": "1 Main Street",
    "city": "London",
    "postcode": "E1 1AA",
    "country": "GB"
  }
}
```

### Upsert availability
```json
{
  "day": "2026-02-20",
  "startHour": 9,
  "endHour": 17,
  "status": "FREE"
}
```

### Create booking
```json
{
  "clientId": "<client-uuid>",
  "professionalId": "<professional-uuid>",
  "addressId": "<client-address-uuid>",
  "startTs": "2026-02-20T10:00:00Z",
  "endTs": "2026-02-20T12:00:00Z"
}
```

### Reject booking
```json
{
  "reason": "Unavailable"
}
```

### Cancel booking
```json
{
  "actorUserId": "<client-or-professional-uuid>",
  "cancelledBy": "CLIENT"
}
```

## Notes
- JWT login and endpoint authorization use JWT `userType` roles for the MVP.
- API currently trusts IDs passed into endpoints (suitable for internal POC testing only).
