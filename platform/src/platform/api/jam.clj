(ns platform.api.jam
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [songpark.common.taxonomy.jam]
            [platform.db.store :as db]))

(defn- <-jam-uuid []
  (uuid/v4))

;; on connection, the frontend sends a request to start
;; a jam session with a given collection of teleporter uuids.
;; this will then initiate the registration of SIP accounts for
;; the given teleporters, which will be sent to the teleporters
;; via MQTT, such that they can call each other. Only when confirmation
;; from all teleporters of SIP connectivity has been received, we send back
;; a response to the frontend with jam/uuid, which will be used as a jam topic
;; on mqtt

(defn- uuid->sip [uuid]
  (let []))

(defn connect [{:keys [data parameters]}]
  (let [uuids (-> parameters :body)
        jam-id (uuid/v4)]
    ;;(log/debug {jam-id (->> uuids (map :teleporter/uuid))})
    (db/wr [:jam jam-id] assoc (->> uuids (map :teleporter/uuid)))
    {:status 200
     :body {:jam/uuid jam-id}}))

(defn disconnect [{:keys [data parameters]}]
  (let [jam-id (-> parameters :body :jam/uuid)]
    (db/wr [:jam] dissoc jam-id)
    {:status 200
     :body ""}))

(comment
  (log/debug (db/rd [:jam]))

  )
