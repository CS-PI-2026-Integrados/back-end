-- MySQL: aplicar uma vez no banco existente antes de publicar a API atualizada.
-- Registros antigos mantêm a indicação false; não inferir declarações históricas.
ALTER TABLE convicted
    ADD COLUMN no_process_declared BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attendance
    MODIFY COLUMN process_id BIGINT NULL,
    ADD COLUMN no_process_declared BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT chk_attendance_process_declaration CHECK (
        (process_id IS NULL AND no_process_declared = TRUE)
        OR (process_id IS NOT NULL AND no_process_declared = FALSE)
    );
