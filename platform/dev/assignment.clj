(ns assignment
  (:require [platform.init :as init]
            [ez-database.core :as db]))




(comment

  ;; delete all assignment results
  (let [db (get-in @init/system [:database])]
    (doseq [table [:audio_audio
                   :result_transcriptions
                   :transcription_transcription
                   :result_result]]
      (db/query! db {:delete-from table})))

  )
