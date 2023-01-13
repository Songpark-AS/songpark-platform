CREATE TABLE auth_user (
  id serial primary key,
  created_at timestamp default now(),
  updated_at timestamp default now(),
  email text NOT NULL UNIQUE,
  password text not null

);
--;;
CREATE TRIGGER auth_user_trigger_updated_at
  BEFORE UPDATE
  ON auth_user
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
