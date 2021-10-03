(ns platform.api.teleporter
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]))

(defn- ns-uuid<- [name]
  (uuid/v5 uuid/+namespace-url+ name))

(defn init [{:keys [data parameters]}]
  (let [{:teleporter/keys [mac nickname]} (:body parameters)
        uuid (ns-uuid<- mac)]
    (if mac
      (do
        (db/wr [:teleporter mac] {:teleporter/uuid uuid
                                  :teleporter/mac mac
                                  :teleporter/nickname nickname})
        (send-message! {:message/type :platform.cmd/subscribe
                        :message/meta {:mqtt/topics {(str uuid) 0}}})
        {:status 200
         :body {:teleporter/uuid uuid}})
      {:status 400
       :body {:error/message "Illegal MAC address"}})))

(defn terminate [{:keys [mac]}teleporter]
  (db/wr [:teleporter] mac dissoc))


(comment
  (log/debug (db/rd [:teleporter]))


  (db/rd [:teleporter])

  (init {:data nil
         :parameters {:body {:teleporter/mac "78:8a:20:fe:79:be"
                             :teleporter/nickname "zedboard-01"}}})

  )
