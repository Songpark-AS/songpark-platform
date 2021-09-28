(ns platform.message
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [songpark.common.protocol.message :as protocol.message]
            [platform.message.handler.incoming :as handler.incoming]
            [platform.message.handler.outgoing :as handler.outgoing]))

(defonce ^:private store (atom nil))

(defn handle-message [msg]
  (let [messenger @store
        injections (-> messenger
                       (select-keys (:injection-ks messenger))
                       (assoc :messenger messenger))]    
    (handler.incoming/incoming (merge msg injections))))

(defn send-message!* [messenger msg]
  (let [injections (-> messenger
                       (select-keys (:injection-ks messenger))
                       (assoc :messenger messenger))]
    (handler.outgoing/outgoing (merge msg injections))))

(defrecord MessageService [injection-ks started? mqtt]
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
