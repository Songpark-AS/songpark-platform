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
    #_(db/query! db {:insert-into :teleporter_teleporter
                     :values values})
    (spit
     "teleporters.sql"
     (clojure.string/join
      "\n"
      (map (fn [{:keys [serial id]}]
             (format "INSERT INTO teleporter_teleporter (id, serial) VALUES ('%s', '%s');" id serial))
           values)))
    )

  (doseq [n [9001 9002]]
    (let [serial (get-serial n)
          id (serial->uuid serial)]
      (println (format "INSERT INTO teleporter_teleporter (id, serial) VALUES ('%s', '%s');" id serial))))
  )
