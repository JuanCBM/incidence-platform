CREATE TABLE usuarios (
    id          BIGSERIAL       PRIMARY KEY,
    nombre      VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    rol         VARCHAR(50)     NOT NULL,
    fecha_alta  DATE            NOT NULL DEFAULT CURRENT_DATE
);

CREATE INDEX idx_usuarios_email ON usuarios (email);
CREATE INDEX idx_usuarios_rol   ON usuarios (rol);
