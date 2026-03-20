CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO admin_user (username, password_hash, role, is_active, created_at)
VALUES (
    'artem',
    crypt('Artem1945', gen_salt('bf', 10)),
    'SUPER_ADMIN',
    TRUE,
    NOW()
)
ON CONFLICT (username) DO UPDATE
SET role = EXCLUDED.role,
    is_active = EXCLUDED.is_active;

