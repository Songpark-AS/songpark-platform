ALTER TABLE auth_user ADD COLUMN token text NULL UNIQUE;
--;;
ALTER TABLE auth_user ADD COLUMN token_at timestamp null;
