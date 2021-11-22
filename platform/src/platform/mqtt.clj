(ns platform.mqtt
  (:require [com.stuartsierra.component :as component]
            [cognitect.transit :as transit]
            [taoensso.timbre :as log]
            [songpark.common.communication :refer [write-handlers]]
            [songpark.common.protocol.mqtt.manager :as protocol.mqtt.manager]
            [platform.mqtt.client :as mqtt.client])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))


(defonce ^:private store (atom nil))
(defonce ^:private counter (atom 0))

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
            (->> (merge {:message/meta {:origin :mqtt :topic topic}}  
                        (<-transit payload)
                        {:mqtt-manager mqtt-manager})
                 (.send-message! (:message-service @store))))]
    (.subscribe client topics on-message)))

(defn- unsubscribe* [{:keys [client] :as mqtt-manager} topics]
  (.unsubscribe client topics))

(defn- publish* [{:keys [client] :as mqtt-manager} topic msg]
  (.publish client topic (->transit (assoc msg :message/id @counter)))
  (swap! counter inc))

(defrecord MQTTManager [injection-ks started? config message-service]
  component/Lifecycle
  (start [this]
    (if started?
      this      
      (do
        (log/debug "Starting MQTTManager")
        (let [new-this (assoc this
                              :started? true
                              :client (mqtt.client/create config))]
          (reset! store new-this)
          new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/debug "Stopping MQTTManager")
          (let [new-this (assoc this :started? false)]
            (when (.connected? (:client this))            
              (.disconnect (:client this)))
            (reset! store new-this)
            new-this))))

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

  (->transit {:teleporter/mac "78:8a:20:fe:79:be",
              :teleporter/nickname "zedboard-01"})



  (-> @store)

  (.connected? (:client @store))
  (.disconnect (:client @store))
  
  (subscribe* @store {"4577bed8-08b7-54cf-ae89-2061ef434b2f" 0})


  (.send-message! (:message-service @store)
                  (->> (merge {:message/type :debug
                               :message/meta {:origin :mqtt :topic "test"}}                        
                              {:message/body {:this 323}}
                              {:mqtt-manager @store})))
  
  (.send-message! (:message-service @store) {:message/type :platform.cmd/subscribe
                                             :message/topics {"4577bed8-08b7-54cf-ae89-2061ef434b2f" 0}})

  (.subscribe (:client @store) {"4577bed8-08b7-54cf-ae89-2061ef434b2f" 0} #(println %1))

  
  (.unsubscribe (:client @store) "4577bed8-08b7-54cf-ae89-2061ef434b2f")
  )
