(ns platform.message.handler.outgoing
  (:require [taoensso.timbre :as log]))


(defmulti outgoing :message/type)

(defmethod outgoing :platform.cmd/subscribe [{:message/keys [topics]
                                              :keys [mqtt-manager]}]
  (log/debug :outgoing (str "Subscribing to " (keys topics)))
  (.subscribe mqtt-manager topics))

(defmethod outgoing :platform.cmd/unsubscribe [{:message/keys [topics]
                                                :keys [mqtt-manager]}]
  (log/debug :outgoing (str "Unsubscribing from " (keys topics)))
  (.unsubscribe mqtt-manager topics))

(defmethod outgoing :teleporter.msg/info [{:message/keys [topic body]
                                           :keys [mqtt-manager]}]
  (.publish mqtt-manager topic body))

(defmethod outgoing :default [{:message/keys [type] :as message}]
  (throw
   (ex-info (str "No message handler defined for message type " type) message)))



