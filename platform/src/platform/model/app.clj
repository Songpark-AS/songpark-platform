(ns platform.model.app
  (:require [platform.model.pairing :as model.pairing]
            [platform.tpstatus :as tpstatus]
            [songpark.jam.platform.protocol :as proto]))


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

(defn get-teleporters [db memdb user-id]
  (let [pairs (->> (model.pairing/get-pairs db user-id)
                   (map (juxt :teleporter/id identity))
                   (into {}))
        pair-ids (->> pairs
                      (map (comp :teleporter/id second))
                      (into #{}))
        jams (->> (proto/read-db memdb [:jam])
                  (vals)
                  (map #(select-keys % [:jam/id :jam/members :jam/status])))
        waiting (proto/read-db memdb [:waiting])]
    (->> (proto/read-db memdb [:teleporter])
         (filter (fn [[tp-id _]]
                   (pair-ids tp-id)))
         (mapv (fn [[k v]]
                 (merge
                  (get pairs k)
                  (dissoc v
                          :teleporter/heartbeat-timestamp
                          :teleporter/mac
                          :teleporter/sip)
                  {:teleporter/online? (tpstatus/teleporter-online? k)
                   :jam/status (get-jam-status jams waiting k)}))))))

(defn app-status [db memdb user-id]
  (let [jams (->> (proto/read-db memdb [:jam])
                  (vals)
                  (map #(select-keys % [:jam/id :jam/members :jam/status])))
        teleporters (get-teleporters db memdb user-id)]
    {:teleporter/teleporters teleporters
     :jam/jams jams}))



(comment

  (->>
   (let [db (:database @platform.init/system)
         memdb (get-in @platform.init/system [:http-server :db])]
     (get-teleporters db memdb 1)
     #_(proto/read-db memdb [:teleporter]))
   (map (juxt :fx.input1/gain)))
  )
