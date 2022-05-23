ALTER TABLE auth_user ADD COLUMN token UUID NULL;
--;;
ALTER TABLE auth_user ADD COLUMN token_at timestamp null;
