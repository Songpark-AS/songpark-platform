(ns platform.message.dispatch.teleporter
  (:require [platform.message.dispatch.interface :as message]
            [platform.tpstatus :as tpstatus]
            [taoensso.timbre :as log]))



(defmethod message/dispatch :teleporter.cmd/disconnect [{:message/keys [body]
                                                         :keys [message-service mqtt-manager]}]
  ;; remove teleporter from global store
  ;; send a service response to topic (might be jam)
  )

;; send an informational message to teleporter topics 
(defmethod message/dispatch :teleporter.msg/info [{:message/keys [body]
                                                   :keys [mqtt-manager]}]
  (let []
    (log/debug body))

  #_(.publish mqtt-manager topics body))


(defmethod message/dispatch :teleporter/heartbeat [{:message/keys [body]}]
  (let [tp-id (:teleporter/id body)]
    (tpstatus/handle-teleporter-heartbeat tp-id)))


(comment
  ;; MESSAGE FORMAT
  {:message/type :some/key

   :message/body
   ;; example body
   {:mqtt/topic ""
    :mqtt/payload {}}

   :message/meta {:origin ""
                  :reply-to []}}

  )
