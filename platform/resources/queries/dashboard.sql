-- name: sql-informational-examinations

WITH informationals AS (
  SELECT
    examination_id, array_agg(informational_id) AS informational_ids
  FROM
    examination_informational
  GROUP BY
    examination_id
)
SELECT
  e.*,
  CASE WHEN i.informational_ids IS NOT NULL
    THEN
      i.informational_ids
    ELSE
      '{}'
    END
  AS informational_ids
FROM
  examination_examination e
  INNER JOIN track_examinations te ON e.id = te.examination_id
  LEFT JOIN informationals i ON e.id = i.examination_id
WHERE
  te.track_id = :track_id;


-- name: sql-full-examinations

WITH informationals AS (
  SELECT
    ei.examination_id, array_agg(ei.informational_id) AS informational_ids
  FROM
    examination_informational ei
    INNER JOIN informational_informational i ON ei.informational_id = i.id
  WHERE
    i.locale = :locale
  GROUP BY
    ei.examination_id
), words AS (
  SELECT t.examination_id,
         json_agg(json_build_object('serial', t.serial, 'word_ids', t.word_ids)) AS word_ids
  FROM
  (
    SELECT examination_id, serial, array_agg(word_id) AS word_ids
    FROM examination_words
    GROUP BY examination_id, serial
  ) t
  GROUP BY
    t.examination_id
)
SELECT
  e.*,
  te.track_id AS track_id,
  CASE WHEN i.informational_ids IS NOT NULL
    THEN
      i.informational_ids
    ELSE
      '{}'
    END
  AS informational_ids,
  CASE WHEN w.word_ids IS NOT NULL
    THEN
      w.word_ids
    ELSE
      '{}'
    END
  AS word_ids
FROM
  examination_examination e
  INNER JOIN track_examinations te ON e.id = te.examination_id
  INNER JOIN track_track t ON te.track_id = t.id
  LEFT JOIN informationals i ON e.id = i.examination_id
  LEFT JOIN words w ON e.id = w.examination_id
WHERE
  t.active_p = true
ORDER BY
  te.track_id ASC,
  te.ordering ASC;

-- name: sql-dashboard-student-measures

SELECT
  dep.assignment_id,
  dep.examination_id,
  coalesce( ww.text, qq.transcript) as word_text,
  ss.score AS scoring_score,
  ss.text AS scoring_text
FROM
  dashboard_examinations_pass dep
  LEFT JOIN examination_words ew
          ON dep.examination_id = ew.examination_id
          AND dep.examination_serial = ew.serial
  LEFT JOIN word_word ww ON ew.word_id = ww.id
  LEFT JOIN examination_questions eq
          ON dep.examination_id = eq.examination_id
          AND dep.examination_serial = eq.serial
  LEFT JOIN question_question qq ON qq.id = eq.question_id
  LEFT JOIN result_result rr ON dep.assignment_id = rr.assignment_id
		  AND dep.student_id = rr.student_id
		  AND coalesce(ww.id,qq.id) = coalesce( rr.word_id, rr.question_id)
  LEFT JOIN scoring_scoring ss ON rr.id = ss.result_id
WHERE
  dep.school_id = :school_id
  AND dep.student_id = :student_id
  AND dep.assignment_id = (SELECT max(assignment_id)
                           FROM dashboard_examinations_pass
                           WHERE student_id = :student_id
                           AND received_p = true
                           AND passed_p = false)
  AND rr.id is not null -- patch will fail if student takes exam under two different locales
ORDER BY
  dep.assignment_id,
  ew.ordering ASC;

