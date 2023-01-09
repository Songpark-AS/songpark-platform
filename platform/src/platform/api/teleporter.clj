(ns platform.api.teleporter
  (:require [platform.config :refer [config]]
            [platform.room :as room]
            [platform.model.pairing :as model.pairing]
            [platform.model.teleporter :as model.teleporter]
            [platform.mqtt.action.pairing :refer [publish-unpaired]]
            [platform.util :refer [serial->uuid]]
            [songpark.jam.platform.protocol :as proto]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [heartbeat-topic]]
            [taoensso.timbre :as log]
            [tick.core :as t]))


(defn- get-public-ip [{:keys [remote-addr headers]}]
  (get headers "x-real-ip" remote-addr))

(defn init [{:keys [data parameters] :as request}]
  (let [public-ip (get-public-ip request)
        {:teleporter/keys [serial local-ip] :as teleporter} (:body parameters)
        id (serial->uuid serial)
        {memdb :db
         roomdb :roomdb
         db :database
         mqtt-client :mqtt-client} data]
    (if serial
      (do
        ;; write to our in memory db
        (proto/write-db memdb [:teleporter id] (assoc teleporter
                                                      :teleporter/id id
                                                      :teleporter/public-ip public-ip
                                                      :teleporter/local-ip local-ip
                                                      :teleporter/heartbeat-timestamp (t/now)))
        ;; subscribe to the heartbeat topic
        (let [topic (heartbeat-topic id)
              mqtt-client (:mqtt-client data)]
          (mqtt/subscribe mqtt-client topic 2))

        ;; cut any previous pairing
        (let [user-ids (model.pairing/cut-pairing db id)]
          ;; inform any application listening that the teleporter has been unpaired
          (publish-unpaired mqtt-client id user-ids))

        ;; close any rooms
        (let [room-id (room/get-room-id roomdb id)]
          (room/db-close roomdb room-id))

        {:status 200
         :body {:teleporter/id id
                :teleporter/ip public-ip}})
      {:status 400
       :body {:error/message "Teleporter is missing a serial"}})))

(defn update [{:keys [data parameters]}]
  (let [{:teleporter/keys [id settings] :as teleporter} (:body parameters)
        db (:db data)]
    (if id
      (let [teleporter (proto/read-db db [:teleporter id])]
        (proto/write-db db [:teleporter id] (merge teleporter settings))
        {:status 200
         :body {:teleporter/id id}})
      {:status 400
       :body {:error/message "Unable to update the Teleporter"}})))

(defn settings [{{db :database} :data
                 {data :body} :parameters
                 {user-id :auth.user/id} :identity
                 :as request}]
  (let [result (model.teleporter/save-settings db user-id data)]
    (if result
      {:status 200
       :body result}
      {:status 400
       :body {:error/message "Unable to save the settings for the Teleporter"}})))


(comment
  (log/debug (db/rd [:teleporter]))


  (db/rd [:teleporter])

  (init {:data nil
         :parameters {:body {:teleporter/mac "78:8a:20:fe:79:be"
                             :teleporter/nickname "zedboard-01"}}})

  )
