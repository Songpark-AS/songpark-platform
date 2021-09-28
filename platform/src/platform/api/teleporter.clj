(ns platform.api.teleporter
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.common.taxonomy.teleporter]
            [platform.db.store :as db]
            [platform.api :refer [send-message!]]
            [platform.message.handler.outgoing :as handler]))

(defn- ns-uuid<- [name]
  (uuid/v5 uuid/+namespace-url+ name))

(defn init [{:keys [data parameters]}]
  (let [mac (-> parameters :body :teleporter/mac)
        uuid (ns-uuid<- mac)]
    (if mac
      (do
        (db/wr [:teleporter mac] {:teleporter/uuid uuid
                                  ;; TODO: deal with nickname
                                  :teleporter/nickname nil})
        (send-message! {:message/type :platform.cmd/subscribe
                        :message/topics {(str uuid) 0}})
        {:status 200
         :body {:teleporter/uuid uuid}})
      {:status 400
       :body {:error/message "Illegal MAC address"}})))

(defn terminate [{:keys [mac]}teleporter]
  (db/wr [:teleporter] mac dissoc))


(comment


  )
