CREATE TABLE teleporter_teleporter (
  id UUID primary key,
  serial text NOT NULL,
  created_at timestamp default now(),
  updated_at timestamp default now()
);
--;;
CREATE TABLE teleporter_pairing (
  user_id INTEGER REFERENCES auth_user(id),
  teleporter_id UUID REFERENCES teleporter_teleporter(id),
  UNIQUE (user_id, teleporter_id)
);
--;;
CREATE TABLE teleporter_settings (
  user_id INTEGER REFERENCES auth_user(id),
  teleporter_id UUID REFERENCES teleporter_teleporter(id),
  created_at timestamp default now(),
  updated_at timestamp default now(),
  name text not null
);
--;;
CREATE TRIGGER teleporter_teleporter_trigger_updated_at
  BEFORE UPDATE
  ON teleporter_teleporter
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
--;;
CREATE TRIGGER teleporter_settings_trigger_updated_at
  BEFORE UPDATE
  ON teleporter_teleporter
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
