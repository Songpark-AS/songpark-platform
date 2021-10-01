(ns platform.mqtt
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [songpark.common.protocol.mqtt.manager :as protocol.mqtt.manager]
            [platform.mqtt.client :as mqtt.client]
            [songpark.common.communication :refer [write-handlers]]
            [platform.message :refer [handle-message]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))


;; transit reader/writer from/to string, since
;; mosquitto does not know anything about transit
(defn- ->transit [v]
  (let [out (ByteArrayOutputStream. 4096)]
    (transit/write (transit/writer out :json {:handlers write-handlers}) v)
    (.toString out "utf-8")))

(defn- <-transit [b]
  (try
    (transit/read (transit/reader (ByteArrayInputStream. b) :json))
    (catch Exception e (do (log/warn "Message not in transit format")
                           (apply str (map char b))))))

(defn- subscribe* [{:keys [client] :as mqtt-manager} topics]
  (letfn [(on-message [^String topic _ ^bytes payload]            
            (->> (merge (<-transit payload) {:message/topic topic})
                 handle-message))]
    (.subscribe client topics on-message)))

(defn- unsubscribe* [{:keys [client] :as mqtt-manager} topics]
  (.unsubscribe client topics))

(defn- publish* [{:keys [client] :as mqtt-manager} topic msg]
  (.publish client topic (->transit msg)))

(defrecord MQTTManager [injection-ks started? config]
  component/Lifecycle
  (start [this]
    (if started?
      this      
      (do
        (log/debug "Starting MQTTManager")
        (assoc this
               :started? true
               :client (mqtt.client/create config)))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/debug "Stopping MQTTManager")
          (when (.connected? (:client this))            
            (.disconnect (:client this)))
          (assoc this :started? false))))

  protocol.mqtt.manager/IMqttManager
  (subscribe [this topics]
    (subscribe* this topics))
  (unsubscribe [this topics]
    (unsubscribe* this topics))
  (publish [this topic msg]
    (publish* this topic msg)))

(defn mqtt-manager [settings]
  (map->MQTTManager settings))


(comment
  (<-transit {:message/type :teleporter.msg/info
              :message/body "This is a message"})
  
  )
