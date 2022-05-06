-- table used for the keys that are possible for FX
-- see git commit for more commentary on the why of it
CREATE TABLE fx_key (
  id serial primary key,
  name text not null
);
--;;
-- the table holding the values
CREATE TABLE fx_value (
  fx_key integer references fx_key(id),
  value integer not null
);
--;;
CREATE TABLE fx_preset (
  id serial primary key,
  user_id integer references auth_user(id),
  created_at timestamp default now(),
  updated_at timestamp default now(),
  slot integer not null default 0 -- 0 is the currently active
);
--;;
CREATE TABLE fx_preset_values (
  value_id integer references fx_value(id),
  preset_id integer references fx_present(id)
);
--;;
CREATE TRIGGER fx_preset_trigger_updated_at
  BEFORE UPDATE
  ON fx_preset
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
