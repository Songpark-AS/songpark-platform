(ns platform.api.teleporter
  (:require [clj-uuid :as uuid]
            [platform.config :refer [config]]
            [songpark.jam.platform.protocol :as proto]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [heartbeat-topic]]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t]))

(defn- ns-uuid<- [name]
  (uuid/v5 uuid/+namespace-url+ name))

(defn init [{:keys [data parameters]}]
  (let [{:teleporter/keys [mac] :as teleporter} (:body parameters)
        id (ns-uuid<- mac)
        sips (:sips config)
        db (:db data)]
    (if mac
      (do
        (proto/write-db db [:teleporter id] (assoc teleporter
                                                   :teleporter/id id
                                                   :teleporter/heartbeat-timestamp (t/now)
                                                   :teleporter/sip (sips id)))
        (let [topic (heartbeat-topic id)
              mqtt-client (:mqtt-client data)]
          (mqtt/subscribe mqtt-client topic 0))
        {:status 200
         :body {:teleporter/id id}})
      {:status 400
       :body {:error/message "Illegal MAC address"}})))

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
