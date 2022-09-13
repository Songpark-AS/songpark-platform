(ns platform.mqtt.handler.jam
  (:require [ez-database.core :as db]
            [platform.util :refer [id->uuid]]
            [songpark.mqtt :as mqtt :refer [handle-message]]))


(defn- get-user-ids [db teleporter-ids]
  (->> {:select [:user_id]
        :from [:teleporter_pairing]
        :where [:in :teleporter_id teleporter-ids]}
       (db/query db)
       (map (comp id->uuid :user_id))))

(defmethod handle-message :jam/started [{:keys [mqtt-client database jam/members] :as msg}]
  (let [stripped-msg (select-keys msg [:jam/id :jam/sip :jam/members :jam/status :message/type])
        user-ids (get-user-ids database members)]
   (doseq [user-id user-ids]
     (mqtt/publish mqtt-client user-id stripped-msg))))

(defmethod handle-message :jam/stopped [_]
  )
