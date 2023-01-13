CREATE TABLE room_room (
  id serial primary key,
  name text NOT NULL UNIQUE,
  created_at timestamp default now(),
  updated_at timestamp default now()
);
--;;
CREATE TABLE room_user (
  room_id INTEGER NOT NULL REFERENCES room_room(id),
  -- Currently just the owner.
  -- This table could be expanded to handle permanent members
  -- and role type based ACL
  user_id INTEGER NOT NULL REFERENCES auth_user(id)
);
--;;
CREATE TRIGGER room_room_trigger_updated_at
  BEFORE UPDATE
  ON room_room
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
