CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    rol        VARCHAR(50)  NOT NULL,
    fecha_alta DATE         NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS incidencias (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo               VARCHAR(255) NOT NULL,
    descripcion          CLOB,
    estado               VARCHAR(50)  NOT NULL,
    prioridad            VARCHAR(50)  NOT NULL,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion  TIMESTAMP    NOT NULL DEFAULT NOW(),
    usuario_id           BIGINT       NOT NULL,
    CONSTRAINT fk_incidencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
