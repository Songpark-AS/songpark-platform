(ns teleporters
  (:require [ez-database.core :as db]
            [platform.init :as init]
            [platform.util :as util :refer [serial->uuid]]))

(defn get-serial [n]
  (format "%04d" n))

(comment

  (let [db (:database @init/system)
        values (->> (range 1 53)
                    (map (fn [n]
                           (let [serial (get-serial n)]
                             {:serial serial
                              :id (serial->uuid serial)}))))]
    (db/query! db {:insert-into :teleporter_teleporter
                   :values values}))
  )
