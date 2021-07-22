ALTER TABLE IF EXISTS tp
DROP IF EXISTS active,
ADD IF NOT EXISTS on_status boolean,
ADD IF NOT EXISTS available_status boolean,
ADD IF NOT EXISTS nick_name text,
ADD IF NOT EXISTS unique_id text;