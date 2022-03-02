(ns platform.api.app
  (:require [platform.tpstatus :as tpstatus]
            [songpark.jam.platform.protocol :as proto]
            [songpark.taxonomy.teleporter]
            [taoensso.timbre :as log]))


;; Highly ad hoc!
(defn connect [{{db :db} :data :as request}]
  (let [jams (->> (proto/read-db db [:jam])
                  (vals)
                  (map #(select-keys % [:jam/id :jam/members])))
        tps (mapv (fn [[k v]]
                    (merge
                     (dissoc v
                             :teleporter/heartbeat-timestamp
                             :teleporter/mac
                             :teleporter/sip)
                     {:teleporter/online? (tpstatus/teleporter-online? k)}))
                  (proto/read-db db [:teleporter]))]
    {:status 200
     :body {:teleporters tps
            :jams jams}}))



(comment

  (let [db (get-in @platform.init/system [:http-server :db])]
    (connect {:data {:db db}}))
  
  )
