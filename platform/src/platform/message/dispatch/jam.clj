(ns platform.message.dispatch.jam
  (:require [platform.message.dispatch.interface :as message]
            [taoensso.timbre :as log]))

(defmethod message/dispatch :jam.cmd/start [{:message/keys [body]
                                             :keys [mqtt-manager]}]
  (log/debug :dispatch (str "Subscribing to " (keys (:mqtt/topics body))))
  #_(.publish mqtt-manager topics body))

(defmethod message/dispatch :jam.cmd/stop [{:message/keys [body]
                                            :keys [mqtt-manager]}]
  (log/debug :dispatch (str "Unsubscribing from " (keys (:mqtt/topics body))))
  #_(.publish mqtt-manager topics body)
  #_(Thread/sleep 5000)
  #_(.unsubscribe mqtt-manager topics))

