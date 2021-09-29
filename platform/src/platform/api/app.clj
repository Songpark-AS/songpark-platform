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
       :body (->> (repeatedly 2 #(rand-nth random-tps))
                  (map-indexed (fn [idx m]
                                 (merge (nth tps idx) m)))         
                  (apply merge random-tps)
                  shuffle)}
      {:status 400
       :body {:error/message "No available teleporters"}})))



(comment

  (first (map #(update-in (random-teleporters 7) [(rand-int 7)]
                          assoc
                          :teleporter/mac (:teleporter/mac %)
                          :teleporter/uuid (:teleporter/uuid %)) (map (fn [[k v]]
                                                                        (merge v {:teleporter/mac k}))
                                                                      (db/rd [:teleporter]))))
  


  
  (let [tps (map (fn [[k v]]
                   (merge v {:teleporter/mac k}))
                 (db/rd [:teleporter]))
        rtp (random-teleporters 7)]
    (->> (repeatedly 2 #(rand-nth rtp))
         (map-indexed (fn [idx m]
                        (merge (nth tps idx) m)))         
         (apply merge rtp)
         shuffle))

  )
