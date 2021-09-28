(ns platform.api.jam
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [songpark.common.taxonomy.jam]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]))


(defn- jam-topics [jam-id]
  (let [uuids (db/rd [:jam jam-id])]
    (->> uuids
         (map #(hash-map (str %) 0))
         (into {}))))

;; on connection, the frontend sends a request to start
;; a jam session with a given collection of teleporter uuids.
;; this will then initiate the registration of SIP accounts for
;; the given teleporters, which will be sent to the teleporters
;; via MQTT, such that they can call each other. Only when confirmation
;; from all teleporters of SIP connectivity has been received, we send back
;; a response to the frontend with jam/uuid, which will be used as a jam topic
;; on mqtt... LATERZ...

(defn connect [{:keys [data parameters]}]
  (let [uuids (-> parameters :body)
        jam-id (uuid/v4)]
    (db/wr [:jam jam-id] (->> uuids (map :teleporter/uuid)))
    (log/debug ::jam-after-dbwr (db/rd [:jam]))
    (if uuids
      (do
        (log/debug :uuids uuids)
        (doseq  [topic (keys (jam-topics jam-id))]
          (log/debug ::topic topic)
          (send-message! {:message/type :teleporter.msg/info
                          :message/topic topic 
                          :message/body {:jam/uuid jam-id}}))
        {:status 200
         :body {:jam/uuid jam-id}})
      {:status 400
       :body {:error/message "Invalid: No members in jam"}})))

(defn disconnect [{:keys [data parameters]}]
  (let [jam-id (-> parameters :body :jam/uuid)]
    (db/wr [:jam] dissoc jam-id)
    {:status 200
     :body ""}))

(comment
  (let [jam-id (first (keys (db/rd [:jam])))]
    (keys (jam-topics jam-id))
    )
  
  )
