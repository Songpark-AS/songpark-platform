(ns platform.mqtt.action.pairing
  (:require [platform.util :refer [id->uuid]]
            [songpark.mqtt :as mqtt]))


(defn publish-unpaired
  "Inform any application listening that the teleporter has been unpaired"
  [mqtt-client teleporter-id user-ids]
  (doseq [user-id user-ids]
    (mqtt/publish mqtt-client
                  (id->uuid user-id)
                  {:message/type :pairing/unpaired
                   :teleporter/id teleporter-id})))
