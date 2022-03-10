(ns platform.api.app
  (:require [platform.tpstatus :as tpstatus]
            [songpark.jam.platform.protocol :as proto]
            [songpark.taxonomy.teleporter]
            [taoensso.timbre :as log]))


(defn- get-jam-jam-status [{:jam/keys [members status]} tp-id]
  (if ((set members) tp-id)
    status
    nil))

(defn- get-jam-status [jams waiting tp-id]
  (let [status (->> jams
                    (map #(get-jam-jam-status % tp-id))
                    (remove nil?)
                    first)]
    (cond
      (and waiting
           (waiting tp-id))
      :jam/waiting

      status
      status

      :else
      :idle)))

(comment
  (let [db (get-in @platform.init/system [:http-server :db])
        jams (->> (proto/read-db db [:jam])
                  (vals))
        waiting (proto/read-db db [:waiting])
        tp-id #uuid "7fdf0551-b5fc-557d-bddc-2ca5b1cdfaa6"]
    (get-jam-status jams waiting tp-id))
  )

;; Highly ad hoc!
(defn connect [{{db :db} :data :as request}]
  (let [jams (->> (proto/read-db db [:jam])
                  (vals)
                  (map #(select-keys % [:jam/id :jam/members :jam/status])))
        waiting (proto/read-db db [:waiting])
        tps (mapv (fn [[k v]]
                    (merge
                     (dissoc v
                             :teleporter/heartbeat-timestamp
                             :teleporter/mac
                             :teleporter/sip)
                     {:teleporter/online? (tpstatus/teleporter-online? k)
                      :jam/status (get-jam-status jams waiting k)}))
                  (proto/read-db db [:teleporter]))]
    {:status 200
     :body {:teleporters tps
            :jams jams}}))



(comment

  (let [db (get-in @platform.init/system [:http-server :db])]
    (connect {:data {:db db}}))
  
  )
