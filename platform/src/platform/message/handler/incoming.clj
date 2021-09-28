(ns platform.message.handler.incoming
  (:require [taoensso.timbre :as log]))


(defmulti incoming :message/type)

(defmethod incoming :teleporter.cmd/disconnect [{:message/keys [topic]
                                                 :keys [messenger mqtt]}]
  ;; remove teleporter from global store
  ;; send a service response to topic (might be jam)
  )


(defmethod incoming :debug/info [{:keys [message/topic message/body]}]
  (log/debug :incoming.debug [topic body]))

(defmethod incoming :default [message]
  (let [msg-type (:message/type message)]
    (throw (ex-info (str "No message handler exist for message type " msg-type) message))))




(comment
  (let [{:message/keys [type]
         :keys [messenger mqtt]} {:message/type :test
                                  :messenger {}
                                  :mqtt {}}]
    [type messenger mqtt])
  

  )
