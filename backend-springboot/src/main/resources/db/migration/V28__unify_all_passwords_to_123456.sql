-- V28: Unify passwords for all user roles (Admin, Creator, Explorer, Viewer) to '123456'
-- Generated using Spring Security BCryptPasswordEncoder (strength 10)
UPDATE users
SET password_hash = '$2a$10$VNOmckiKLrgEsdc.3hwZ.eFeShtPnbEAKzBqRM/IhnuYFUebyPIdK',
    must_change_password = FALSE,
    terms_accepted_at = COALESCE(terms_accepted_at, now()),
    status = 'ACTIVE',
    updated_at = now()
WHERE deleted_at IS NULL;

-- Clear any recorded failed login attempts so all accounts can log in immediately
DELETE FROM login_attempts;
