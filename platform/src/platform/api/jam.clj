(ns platform.api.jam
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [songpark.common.taxonomy.jam]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]))


(defn- members [jam-id]
  (let [uuids (db/rd [:jam jam-id])]
    (->> uuids
         (mapv #(str %)))))


;; on connection, the frontend sends a request to start
;; a jam session with a given collection of teleporter uuids.
;; this will then initiate the registration of SIP accounts for
;; the given teleporters, which will be sent to the teleporters
;; via MQTT, such that they can call each other. Only when confirmation
;; from all teleporters of SIP connectivity has been received, we send back
;; a response to the frontend with jam/uuid, which will be used as a jam topic
;; on mqtt... LATERZ...

(defn start [{:keys [data parameters]}]
  (let [uuids (-> parameters :body)
        jam-id (uuid/v4)]
    (db/wr [:jam jam-id] (->> uuids (map :teleporter/uuid)))
    (if-not (empty? uuids)
      (do
        (send-message! {:message/type :jam.cmd/start
                        :message/body {:jam/status true
                                       :jam/topic (str jam-id)
                                       :jam/members (members jam-id)}
                        :message/meta {:mqtt/topic (str jam-id)
                                       :origin :platform}})
        {:status 200
         :body {:jam/uuid jam-id
                :jam/status true}})
      {:status 400
       :body {:error/message "Invalid: No members in jam"}})))

(defn stop [{:keys [data parameters]}]
  (let [jam-id (-> parameters :body :jam/uuid)
        res (->> (db/wr [:jam] dissoc jam-id) :jam keys (filter #{jam-id}))]        
    (if-not (empty? res)
      {:status 200
       :body ""}
      {:status 400
       :body {:error/message "Could not stop jam"}})))



(comment
  (let [jam-id (first (keys (db/rd [:jam])))]
    (keys (jam-topics jam-id))
    )

  (db/rd [:teleporter])
  (db/rd [:jam])

  )

