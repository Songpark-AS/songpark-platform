(ns platform.mqtt.handler.teleporter
  (:require [platform.tpstatus :as tpstatus]
            [songpark.mqtt :refer [handle-message]]
            [taoensso.timbre :as log]))

(defmethod handle-message :teleporter/heartbeat [{:keys [teleporter/id]}]
  #_(log/debug "Heartbeat from teleporter" id)
  (tpstatus/handle-teleporter-heartbeat id))
