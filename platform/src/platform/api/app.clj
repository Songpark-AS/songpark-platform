(ns platform.api.app
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.util.mock :refer [random-teleporters]]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]))

(defn- unique-teleporters [tps n]
  (let [rtp (random-teleporters (- n (count tps)))]
    (shuffle (vec (concat rtp tps)))))

;; Highly ad hoc!
(defn connect [request]
  (let [tps (mapv (fn [[k v]]
                    (merge v {:teleporter/mac k}))
                  (db/rd [:teleporter]))]
    {:status 200
     :body []}
    #_(if-not (empty? tps)
      {:status 200
       :body (unique-teleporters tps 7)}
      {:status 400
       :body {:error/message "No available teleporters"}})))



(comment

  (db/rd [:teleporter])
  (db/rd [:jam]) 
  
  )
