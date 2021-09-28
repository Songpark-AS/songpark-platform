(ns platform.message.handler.outgoing
  (:require [taoensso.timbre :as log]))


(defmulti outgoing :message/type)

(defmethod outgoing :platform.cmd/subscribe [{:message/keys [topics]
                                              :keys [mqtt]}]
  (log/debug :outgoing (str "Subscribing to " (keys topics)))
  (.subscribe mqtt topics))

(defmethod outgoing :platform.cmd/unsubscribe [{:message/keys [topics]
                                                :keys [mqtt]}]
  (log/debug :outgoing (str "Unsubscribing from " (keys topics)))
  (.unsubscribe mqtt topics))

(defmethod outgoing :default [message]
  (let [msg-type (:message/type message)]
    (throw (ex-info (str "No message handler exist for message type " msg-type) message))))
