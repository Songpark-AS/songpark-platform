CREATE TABLE profile_pronoun (
  id serial primary key,
  name text not null
);
--;;
INSERT INTO profile_pronoun VALUES (-1, 'Unknown');
--;;
ALTER TABLE profile_profile ADD COLUMN pronoun_id INTEGER NOT NULL DEFAULT -1 REFERENCES profile_pronoun(id);
