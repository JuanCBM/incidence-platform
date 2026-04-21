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

CREATE TABLE IF NOT EXISTS sugerencia_ia (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    incidencia_id     BIGINT NOT NULL,
    prompt_ejecutado  CLOB,
    respuesta         CLOB,
    fecha_generacion  TIMESTAMP,
    CONSTRAINT fk_sugerencia_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias(id)
);
