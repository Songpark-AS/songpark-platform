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
  (let [{:teleporter/keys [mac nickname tpx-version bp-version fpga-version apt-version]} (:body parameters)
        id (ns-uuid<- mac)
        sips (:sips config)
        db (:db data)]
    (if mac
      (do
        (proto/write-db db [:teleporters id] {:teleporter/id id
                                              :teleporter/mac mac
                                              :teleporter/heartbeat-timestamp (t/now)
                                              :teleporter/sip (sips id)
                                              :teleporter/nickname nickname
                                              :teleporter/tpx-version tpx-version
                                              :teleporter/bp-version bp-version
                                              :teleporter/fpga-version fpga-version
                                              :teleporter/apt-version apt-version})
        (let [topic (heartbeat-topic id)
              mqtt-client (:mqtt-client data)]
          (mqtt/subscribe mqtt-client topic 0))
        {:status 200
         :body {:teleporter/id id}})
      {:status 400
       :body {:error/message "Illegal MAC address"}})))


(comment
  (log/debug (db/rd [:teleporter]))


  (db/rd [:teleporter])

  (init {:data nil
         :parameters {:body {:teleporter/mac "78:8a:20:fe:79:be"
                             :teleporter/nickname "zedboard-01"}}})

  )
