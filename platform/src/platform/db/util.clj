(ns platform.db.util
  (:require [platform.db.store :as db]))

(defn all-topics []
  (let [tp-topics (->> (db/rd [:teleporter]) (map :teleporter/uuid))
        jams (db/rd [:jam])]
    [tp-topics jam-topics]))


(comment
  (all-topics)

  )
