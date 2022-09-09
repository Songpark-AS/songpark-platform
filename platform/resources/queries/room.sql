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
LEFT JOIN room_session_users rsu ON s.id = rsu.room_session_id
LEFT JOIN profile_profile pp ON rsu.user_id = pp.user_id
WHERE ru.user_id = :user_id
GROUP BY rr.id, ru.user_id;


-- name: sql-get-jammed
WITH session_details AS (
  SELECT  rs.room_id,
          rs.created_at AS last_jammed,
          rs.id AS room_session_id,
          array_agg(pp.name ORDER BY pp.name ASC) FILTER (WHERE pp.name IS NOT NULL) AS jammers
    FROM room_session rs
         INNER JOIN room_session_users rsu on rs.id = rsu.room_session_id
         INNER JOIN profile_profile pp on rsu.user_id = pp.user_id
   GROUP BY rs.room_id, rs.created_at, rs.id
)
SELECT DISTINCT rr.id,
                rr.name_normalized,
                rr.name,
                ru.user_id,
                sd.last_jammed,
                sd.jammers
  FROM room_room rr
       INNER JOIN room_user ru ON rr.id = ru.room_id
       INNER JOIN room_session rs ON rs.room_id = rr.id
       INNER JOIN room_session_users rsu ON rs.id = rsu.room_session_id
       INNER JOIN session_details sd ON sd.room_id = rr.id AND sd.room_session_id = rs.id
 WHERE rsu.user_id = :user_id
 ORDER BY sd.last_jammed desc
 LIMIT 5;
