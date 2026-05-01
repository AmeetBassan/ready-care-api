ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_picture_storage_key TEXT;
