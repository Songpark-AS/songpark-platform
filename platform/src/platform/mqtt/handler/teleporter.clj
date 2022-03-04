(ns platform.mqtt.handler.teleporter
  (:require [platform.tpstatus :as tpstatus]
            [songpark.jam.platform :as jam.platform]
            [songpark.mqtt :refer [handle-message]]
            [taoensso.timbre :as log]))

(defmethod handle-message :teleporter/heartbeat [{:keys [teleporter/id]}]
  (tpstatus/handle-teleporter-heartbeat id))

(defmethod handle-message :jam.teleporter/left [{:keys [jam-manager jam/id] :as msg}]
  (jam.platform/left jam-manager id msg))
