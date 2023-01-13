ALTER TABLE auth_user ADD COLUMN verified_email_token text NULL UNIQUE;
--;;
ALTER TABLE auth_user ADD COLUMN verified_email_token_at timestamp null;
