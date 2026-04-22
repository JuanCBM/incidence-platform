CREATE TABLE incidencias (
    id                   BIGSERIAL       PRIMARY KEY,
    titulo               VARCHAR(255)    NOT NULL,
    descripcion          TEXT,
    estado               VARCHAR(50)     NOT NULL,
    prioridad            VARCHAR(50)     NOT NULL,
    fecha_creacion       TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion  TIMESTAMP       NOT NULL DEFAULT NOW(),
    usuario_id           BIGINT          NOT NULL,
    CONSTRAINT fk_incidencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_incidencias_estado    ON incidencias (estado);
CREATE INDEX idx_incidencias_prioridad ON incidencias (prioridad);
CREATE INDEX idx_incidencias_usuario   ON incidencias (usuario_id);
