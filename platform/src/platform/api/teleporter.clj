(ns platform.api.teleporter
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.api :refer [send-message!]]
            [platform.config :refer [config]]
            [platform.db.store :as db]))

(defn- ns-uuid<- [name]
  (uuid/v5 uuid/+namespace-url+ name))

(defn init [{:keys [data parameters]}]
  (let [{:teleporter/keys [mac nickname tpx-version bp-version fpga-version]} (:body parameters)
        uuid (ns-uuid<- mac)
        sips (:sips config)]
    (if mac
      (do
        (db/wr [:teleporter mac] {:teleporter/uuid uuid
                                  :teleporter/mac mac
                                  :teleporter/sip (sips uuid)
                                  :teleporter/nickname nickname
                                  :teleporter/tpx-version tpx-version
                                  :teleporter/bp-version bp-version
                                  :teleporter/fpga-version fpga-version})
        (send-message! {:message/type :platform.cmd/subscribe
                        :message/meta {:mqtt/topics {(str uuid) 0 (str uuid "/heartbeat") 0}}})
        {:status 200
         :body {:teleporter/uuid uuid}})
      {:status 400
       :body {:error/message "Illegal MAC address"}})))

(defn terminate [{:keys [mac]} teleporter]
  (db/wr [:teleporter] mac dissoc))


(comment
  (log/debug (db/rd [:teleporter]))


  (db/rd [:teleporter])

  (init {:data nil
         :parameters {:body {:teleporter/mac "78:8a:20:fe:79:be"
                             :teleporter/nickname "zedboard-01"}}})

  )
