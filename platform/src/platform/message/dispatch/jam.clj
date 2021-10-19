(ns platform.message.dispatch.jam
  (:require [platform.api :refer [send-message!]]
            [platform.data :as data]
            [platform.message.dispatch.interface :as message]
            [taoensso.timbre :as log]))

(defmethod message/dispatch :jam.cmd/start [{:message/keys [body]
                                             :keys [message-service mqtt-manager]}]
  (data/set-jam-id! (:jam/topic body))
  (let [members (:jam/members body)
        topic (data/get-jam)]
    (log/debug members)
    (log/debug (str "Subscribing to " topic))
    (send-message! {:message/type :platform.cmd/subscribe
                    :message/meta {:mqtt/topics {topic 0}}})
    (doseq [member members]
      (.publish mqtt-manager member {:message/type :jam.cmd/start
                                     :message/body body
                                     :message/meta {:mqtt/topic member
                                                    :origin :platform}}))))

(defmethod message/dispatch :jam.cmd/stop [{:message/keys [body meta]
                                            :keys [mqtt-manager]}]
  (let [jam-topic (data/get-jam)
        teleporters-topic (data/get-jam-teleporters)]
    (log/debug :dispatch (str "Unsubscribing from " jam-topic))
    
    (.publish mqtt-manager teleporters-topic {:message/type :jam.cmd/stop
                                              :message/body body
                                              :message/meta meta})
    (.unsubscribe mqtt-manager [jam-topic])
    (data/clear-jam-id!)))

