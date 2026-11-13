-- MySQL: execute no banco existente antes de publicar a API atualizada.
ALTER TABLE convicted
    MODIFY COLUMN birth_date DATE NULL,
    MODIFY COLUMN phone VARCHAR(11) NULL;

ALTER TABLE attendance
    MODIFY COLUMN phone VARCHAR(11) NULL;
