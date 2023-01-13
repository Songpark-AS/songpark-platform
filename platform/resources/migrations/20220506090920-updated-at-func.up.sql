CREATE OR REPLACE FUNCTION updated_at_trigger_func()
  RETURNS trigger AS
  $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';
