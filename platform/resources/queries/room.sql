-- name: sql-get-rooms

WITH session AS (
    SELECT max(id) AS id, MAX(created_at) AS created_at, room_id
    FROM room_session
    GROUP BY room_id
)
SELECT rr.id,
       rr.name_normalized,
       rr.name,
       ru.user_id,
       MAX(s.created_at) AS last_jammed,
       array_agg(pp.name ORDER BY pp.name ASC) FILTER (WHERE pp.name IS NOT NULL) AS jammers
FROM room_room rr
INNER JOIN room_user ru ON rr.id = ru.room_id
LEFT JOIN session s ON s.room_id = rr.id
LEFT JOIn room_session_users rsu ON s.id = rsu.room_session_id
LEFT JOIN profile_profile pp ON rsu.user_id = pp.user_id
WHERE ru.user_id = :user_id
GROUP BY rr.id, ru.user_id;
