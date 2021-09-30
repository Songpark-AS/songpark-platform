(ns platform.api.app
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.util.mock :refer [random-teleporters]]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]
            [platform.message.handler.outgoing :as handler]))

(defn- unique-teleporters [tps n]
  (let [rtp (random-teleporters n)
        rtp-m (reduce-kv (fn [m _ v] (assoc m (:teleporter/uuid v) v)) {} rtp)
        sel-rtp (take (count tps) (shuffle rtp))
        rtp-m2 (apply (partial dissoc rtp-m) (into #{} (map :teleporter/uuid sel-rtp)))]
    (->> sel-rtp
         (map-indexed (fn [idx m]
                        (merge (nth tps idx) (select-keys m [:teleporter/nickname]))))         
         (concat (vals rtp-m2))
         shuffle)))

;; Highly ad hoc!
(defn connect [request]
  (let [tps (map (fn [[k v]]
                   (merge v {:teleporter/mac k}))
                 (db/rd [:teleporter]))]        
    (if-not (empty? tps)
      {:status 200
       :body (unique-teleporters tps 7)}
      {:status 400
       :body {:error/message "No available teleporters"}})))



(comment
  (map (fn [[k v]]
         (merge v {:teleporter/mac k}))
       (db/rd [:teleporter]))
  
  (let [tps (map (fn [[k v]]
                   (merge v {:teleporter/mac k}))
                 (db/rd [:teleporter]))]
    (->> (unique-teleporters tps 7)
         (map :teleporter/uuid)))

  (unique-teleporters (map (fn [[k v]]
                             (merge v {:teleporter/mac k}))
                           (db/rd [:teleporter])) 6)

  )
