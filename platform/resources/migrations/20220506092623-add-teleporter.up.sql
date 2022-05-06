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
  name text not null default ''
);
--;;
CREATE TRIGGER teleporter_teleporter_trigger_updated_at
  BEFORE UPDATE
  ON teleporter_teleporter
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
