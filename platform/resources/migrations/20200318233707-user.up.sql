CREATE TABLE auth_user (
       id serial not null primary key,
       password text not null,
       email text not null,
       first_name text not null,
       last_name text not null,
       active_p boolean default false
);
