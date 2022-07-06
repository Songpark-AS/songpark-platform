-- table used for the keys that are possible for FX
-- see git commit for more commentary on the why of it
CREATE TABLE fx_key (
  id text primary key
);
--;;
CREATE TABLE fx_preset (
  id serial primary key,
  user_id integer not null references auth_user(id),
  created_at timestamp default now(),
  updated_at timestamp default now(),
  name text not null
);
--;;
-- the table holding the values
CREATE TABLE fx_value (
  id serial primary key,
  fx_key text not null references fx_key(id),
  preset_id integer not null references fx_preset(id),
  value integer not null
);
--;;
CREATE TRIGGER fx_preset_trigger_updated_at
  BEFORE UPDATE
  ON fx_preset
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
