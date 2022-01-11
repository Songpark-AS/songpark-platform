(ns platform.api.app
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.util.mock :refer [random-teleporters]]
            [platform.db.store :as db]
            [platform.tpstatus :as tpstatus]
            [platform.api :refer [send-message!]]))


;; Highly ad hoc!
(defn connect [request]
  (let [tps (mapv (fn [[k v]]
                    (merge v {:teleporter/mac k
                              :teleporter/online? (tpstatus/teleporter-online? (:teleporter/uuid v))}))
                  (db/rd [:teleporter]))]
    {:status 200
     :body tps})
  )



(comment

  (db/rd [:teleporter])
  (db/rd [:jam]) 
  
  )
