-- Se ejecuta en cada arranque (spring.sql.init.mode=always), por eso todo es idempotente.

-- Tabla users
CREATE TABLE IF NOT EXISTS users
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    username
    VARCHAR
(
    40
) NOT NULL,
    password_hash VARCHAR
(
    500
) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    last_password_change DATETIME NOT NULL,
    password_expires_at DATETIME NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_users_username UNIQUE
(
    username
)
    );

-- Tabla roles
CREATE TABLE IF NOT EXISTS roles
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    name
    VARCHAR
(
    50
) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE
(
    name
)
    );

-- Tabla intermedia user_roles (relación N:M entre users y roles)
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id
    BIGINT
    NOT
    NULL,
    role_id
    BIGINT
    NOT
    NULL,
    PRIMARY
    KEY
(
    user_id,
    role_id
),
    CONSTRAINT fk_user_roles_user FOREIGN KEY
(
    user_id
) REFERENCES users
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY
(
    role_id
) REFERENCES roles
(
    id
)
  ON DELETE CASCADE
    );