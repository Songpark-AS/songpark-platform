(ns platform.api.app
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.util.mock :refer [random-teleporters]]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]
            [platform.message.handler.outgoing :as handler]))

;; Highly ad hoc!
(defn connect [request]
  (let [random-tps (random-teleporters 7)
        tps (map (fn [[k v]]
                   (merge v {:teleporter/mac k}))
                 (db/rd [:teleporter]))]        
    (if-not (empty? tps)
      {:status 200
       :body (first (map #(update-in random-tps [(rand-int 3)]
                                     assoc
                                     :teleporter/mac (:teleporter/mac %)
                                     :teleporter/uuid (:teleporter/uuid %)) tps))}
      {:status 400
       :body {:error/message "No available teleporters"}})))



