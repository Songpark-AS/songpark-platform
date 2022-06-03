CREATE TABLE profile_pronoun (
  id serial primary key,
  name text not null
);
--;;
INSERT INTO profile_pronoun VALUES (-1, 'Unknown');
--;;
CREATE TABLE profile_profile (
  id serial primary key,
  created_at timestamp default now(),
  updated_at timestamp default now(),
  user_id integer references auth_user(id) not null,
  name text not null UNIQUE,
  location text not null,
  bio text not null,
  image_url text not null,
  pronoun_id integer references profile_pronoun(id) not null default -1
);
--;;
CREATE TRIGGER profile_profile_trigger_updated_at
  BEFORE UPDATE
  ON profile_profile
  FOR EACH ROW
    EXECUTE PROCEDURE updated_at_trigger_func();
