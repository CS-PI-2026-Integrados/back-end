-- MySQL: aplicar uma vez no banco existente antes de publicar a API atualizada.
-- Não preencher as colunas históricas com o cadastro atual de atendimentos antigos.
ALTER TABLE attendance
    ADD COLUMN receipt_context_version INT NULL,
    ADD COLUMN receipt_convicted_name VARCHAR(255) NULL,
    ADD COLUMN receipt_convicted_cpf VARCHAR(14) NULL,
    ADD COLUMN receipt_process_number VARCHAR(50) NULL,
    ADD COLUMN receipt_district_name VARCHAR(255) NULL,
    ADD COLUMN receipt_operator_name VARCHAR(255) NULL,
    ADD COLUMN receipt_convicted_photo_asset_id BIGINT NULL,
    ADD INDEX idx_attendance_receipt_convicted_photo (receipt_convicted_photo_asset_id),
    ADD CONSTRAINT fk_attendance_receipt_convicted_photo
        FOREIGN KEY (receipt_convicted_photo_asset_id) REFERENCES media_asset (id);
