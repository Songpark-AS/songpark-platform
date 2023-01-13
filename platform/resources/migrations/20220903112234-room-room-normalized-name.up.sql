ALTER TABLE room_room ADD COLUMN name_normalized TEXT NOT NULL DEFAULT '';
--;;
UPDATE room_room SET name_normalized = replace(lower(name), ' ', '-');
--;;
ALTER TABLE room_room ADD CONSTRAINT name_normalized_unique UNIQUE (name);
