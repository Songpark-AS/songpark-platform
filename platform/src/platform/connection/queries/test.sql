-- name: usr-get-all
SELECT * FROM usr;

-- name: usr-clear!
DELETE FROM usr;

-- name: usr-add<!
INSERT INTO usr (e_mail, descr, passwd)
VALUES (:e_mail, :descr, :passwd);

-- name: tp-activitation!
UPDATE tp
SET active = 'true'
WHERE sid = :tpid;

-- name: tp-deactivitation!
UPDATE tp
SET active = 'false'
WHERE sid = :tpid;