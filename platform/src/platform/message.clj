(ns platform.message
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [songpark.common.protocol.message :as protocol.message]
            [platform.message.handler.incoming :as handler.incoming]
            [platform.message.handler.outgoing :as handler.outgoing]))

(defonce ^:private store (atom nil))

(defn handle-message [msg]
  (let [message-service @store
        injections (-> message-service
                       (select-keys (:injection-ks message-service))
                       (assoc :message-service message-service))]    
    (handler.incoming/incoming (merge msg injections))))

(defn send-message!* [message-service msg]
  (let [injections (-> message-service
                       (select-keys (:injection-ks message-service))
                       (assoc :message-service message-service))]
    (handler.outgoing/outgoing (merge msg injections))))

(defrecord MessageService [injection-ks started? mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting MessageService")
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
            new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping MessageService")
          (let [new-this (assoc this
                                :started? false)]
            (reset! store nil)
            new-this))))
  
  protocol.message/IMessageService
  (send-message! [this msg]
    (send-message!* this msg)))

(defn message-service [settings]
  (map->MessageService settings))



(comment
  (prn @store)

  )
