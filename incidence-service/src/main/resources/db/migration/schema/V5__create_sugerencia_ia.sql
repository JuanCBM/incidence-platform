CREATE TABLE sugerencia_ia (
    id                BIGSERIAL    PRIMARY KEY,
    incidencia_id     BIGINT       NOT NULL,
    prompt_ejecutado  TEXT,
    respuesta         TEXT,
    fecha_generacion  TIMESTAMP,
    CONSTRAINT fk_sugerencia_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias(id)
);
