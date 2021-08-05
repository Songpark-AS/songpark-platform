ALTER TABLE IF EXISTS tp
ADD IF NOT EXISTS active boolean,
DROP IF EXISTS on_status,
DROP IF EXISTS available_status,
DROP IF EXISTS nickname,
DROP IF EXISTS unique_id;