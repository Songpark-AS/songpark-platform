(ns platform.message.dispatch.jam
  (:require [platform.message.dispatch.interface :as message]
            [taoensso.timbre :as log]
            [platform.api :refer [send-message!]]))

(defmethod message/dispatch :jam.cmd/start [{:message/keys [body meta]
                                             :keys [message-service mqtt-manager]}]
  (let [members (:jam/members body)]
    (log/debug members)
    (log/debug (str "Subscribing to " (:jam/topic body)))
    (send-message! {:message/type :platform.cmd/subscribe
                    :message/meta {:mqtt/topics {(str (:jam/topic body)) 0}}})
    (doseq [member members]
      (.publish mqtt-manager member {:message/type :debug
                                     :message/body body
                                     :message/meta {:mqtt/topic member
                                                    :origin :platform}}))))

(defmethod message/dispatch :jam.cmd/stop [{:message/keys [body]
                                            :keys [mqtt-manager]}]
  (log/debug :dispatch (str "Unsubscribing from " (keys (:mqtt/topics body))))
  #_(.publish mqtt-manager topics body)
  #_(Thread/sleep 5000)
  #_(.unsubscribe mqtt-manager topics))

