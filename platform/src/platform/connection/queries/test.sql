-- name: tp-set-on!
UPDATE tp
SET on_status = true
WHERE unique_id = :tpid;

-- name: tp-set-off!
UPDATE tp
SET on_status = false
WHERE unique_id = :tpid;

-- name: tp-set-available!
UPDATE tp
SET available_status = true
WHERE unique_id = :tpid;

-- name: tp-set-unavailable!
UPDATE tp
SET available_status=false
WHERE unique_id = :tpid;

-- name: tpid-from-nick
SELECT unique_id FROM tp
WHERE nickname = :nickname;

-- name: tp-get-availability
SELECT available_status FROM tp
WHERE unique_id = :tpid;

-- name: tp-get-on-status
SELECT on_status FROM tp
WHERE unique_id = :tpid;
