(ns platform.message.dispatch.platform
  (:require [platform.message.dispatch.interface :as message]
            [taoensso.timbre :as log]            ))

(defmethod message/dispatch :platform.cmd/subscribe [{:message/keys [body]
                                                      :keys [mqtt-manager] :as message}]
  (log/debug (:client mqtt-manager))
  (let [topics (:mqtt/topics body)]
    (log/debug :dispatch (str "Subscribing to " (keys topics)))
    (.subscribe mqtt-manager topics)))

(defmethod message/dispatch :platform.cmd/unsubscribe [{:message/keys [body]
                                                        :keys [mqtt-manager]}]
  (let [topics (:mqtt/topics body)]
    (log/debug :dispatch (str "Unsubscribing from " (keys topics)))
    (.unsubscribe mqtt-manager topics)))

