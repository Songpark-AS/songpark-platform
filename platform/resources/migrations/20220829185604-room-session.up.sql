CREATE TABLE room_session (
  id serial primary key not null,
  created_at timestamp default now(),
  closed_at timestamp null,
  room_id integer not null references room_room(id)
);
--;;
CREATE TABLE room_session_users (
  room_session_id integer not null references room_session(id),
  user_id integer not null references auth_user(id),
  joined_at timestamp default now(),
  left_at timestamp null
 );
