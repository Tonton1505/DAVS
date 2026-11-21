-- V29: Unify passwords for all user roles (Admin, Creator, Explorer, Viewer) to '12345678'
-- BCrypt hash generated with Spring Security BCryptPasswordEncoder (strength 10)
UPDATE users
SET password_hash = '$2a$10$wdVRTJ0dltibQwbxo1Jy3.nrLVEbDXAhkYHUON3bXoMBqk.BlOjOq',
    must_change_password = FALSE,
    terms_accepted_at = COALESCE(terms_accepted_at, now()),
    status = 'ACTIVE',
    updated_at = now()
WHERE deleted_at IS NULL;

-- Clear any recorded failed login attempts so all accounts can log in immediately
DELETE FROM login_attempts;
