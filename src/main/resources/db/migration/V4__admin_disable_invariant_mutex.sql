CREATE TABLE admin_lock_mutexes (
    lock_key VARCHAR(64) PRIMARY KEY,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO admin_lock_mutexes (lock_key, updated_at)
VALUES ('PLATFORM_ADMIN_DISABLE_GUARD', CURRENT_TIMESTAMP);
