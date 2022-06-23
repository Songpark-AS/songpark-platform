(ns platform.api.teleporter
  (:require [platform.config :refer [config]]
            [platform.util :refer [serial->uuid]]
            [songpark.jam.platform.protocol :as proto]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [heartbeat-topic]]
            [taoensso.timbre :as log]
            [tick.core :as t]))

(defn init [{:keys [data parameters]}]
  (let [{:teleporter/keys [serial] :as teleporter} (:body parameters)
        id (serial->uuid serial)
        db (:db data)]
    (if serial
      (do
        (proto/write-db db [:teleporter id] (assoc teleporter
                                                   :teleporter/id id
                                                   :teleporter/heartbeat-timestamp (t/now)))
        (let [topic (heartbeat-topic id)
              mqtt-client (:mqtt-client data)]
          (mqtt/subscribe mqtt-client topic 2))
        {:status 200
         :body {:teleporter/id id}})
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


(comment
  (log/debug (db/rd [:teleporter]))


  (db/rd [:teleporter])

  (init {:data nil
         :parameters {:body {:teleporter/mac "78:8a:20:fe:79:be"
                             :teleporter/nickname "zedboard-01"}}})

  )
