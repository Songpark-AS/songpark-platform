(ns platform.mqtt.handler.jam
  (:require [ez-database.core :as db]
            [platform.util :refer [id->uuid]]
            [songpark.mqtt :as mqtt :refer [handle-message]]
            [songpark.jam.platform :as jam.platform]
            [taoensso.timbre :as log]
            [songpark.jam.platform :as jam.platform]
            [platform.mqtt.handler.jam :as jam]))


(defn- get-user-ids [db teleporter-ids]
  (->> {:select [:user_id]
        :from [:teleporter_pairing]
        :where [:in :teleporter_id teleporter-ids]}
       (db/query db)
       (map (comp id->uuid :user_id))))

 (defmethod handle-message :jam/joined [{:keys [jam-manager]
                                        teleporter-id :teleporter/id
                                        jam-id :jam/id
                                        :as _msg}]
  (log/debug :jam/joined (select-keys _msg [:message/type
                                            :jam/id
                                            :teleporter/id]))
  (jam.platform/joined jam-manager jam-id teleporter-id))

(defmethod handle-message :jam/left [{:keys [jam-manager]
                                      teleporter-id :teleporter/id
                                      jam-id :jam/id
                                      :as _msg}]
  (log/debug :jam/left (select-keys _msg [:message/type
                                          :jam/id
                                          :teleporter/id]))
  (jam.platform/left jam-manager jam-id teleporter-id))


(defmethod handle-message :jam/event [{:keys [jam-manager]
                                       teleporter-id :teleporter/id
                                       event-type :event/type
                                       event-value :event/value
                                       jam-id :jam/id
                                       :as _msg}]
  (log/debug :jam/event (select-keys _msg [:message/type
                                           :jam/id
                                           :teleporter/id
                                           :event/type
                                           :event/value]))
  (condp = event-type
    ;; :sync/timeout (jam.platform/timed-out jam-manager jam-id teleporter-id)
    :else nil))


(defmethod handle-message :teleporter/reset-success [_msg]
  nil)
