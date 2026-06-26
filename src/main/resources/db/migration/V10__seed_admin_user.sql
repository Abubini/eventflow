-- Admin user: email: admin@eventflow.com / password: admin123
-- Password hash generated with BCrypt (strength 10)
INSERT INTO users (name, email, password_hash, role, active, created_at)
VALUES (
           'Admin',
           'admin@eventflow.com',
           '$2a$10$GGimNUXMmrwSbbIsPitFZexlS2G6yxRCFmr79n7EmINRh9AH1vZbS',
           'STAFF',
           TRUE,
           NOW()
       )
    ON CONFLICT (email) DO NOTHING;