(ns platform.mqtt.client
  (:require [clojurewerkz.machine-head.client :as mh]
            [songpark.common.protocol.mqtt.client :as protocol.mqtt.client]
            [taoensso.timbre :as log]))


(def ^:private default-options
  {:on-connect-complete (fn [& args] (println "Connection completed." args))
   :on-connection-lost (fn [& args] (println "Connection lost." args))
   :on-delivery-complete (fn [& args] (println "Delivery completed." args))
   :on-unhandled-message (fn [& args] (println "Unhandled message encountered." args))})

(defn- gen-uri-string [{:keys [scheme host port]}]
  (str scheme "://" host ":" port))

(defn- gen-paho-options [{:keys [client-id options connect-options]}]
  (-> {:client-id (or client-id (mh/generate-id))}
      (merge (or options default-options))
      (merge {:opts connect-options})))

(defrecord MqttClient [config client]
  protocol.mqtt.client/IMqttClient
  (connect [this]
    ;; only used for when/if client was disconnected after initial creation
    (log/debug "Connecting to broker")
    (.connect (:client this)))

  (connected? [this]
    (mh/connected? (:client this)))

  (publish [this topic message]
    (mh/publish (:client this) topic message))

  (disconnect [this]
    (log/debug "Disconnecting from broker")
    (mh/disconnect (:client this)))

  (subscribe [this topics on-message]
    (when (.connected? this)
      (mh/subscribe (:client this) topics on-message)))

  (unsubscribe [this topics]
    (when (.connected? this)
      (mh/unsubscribe (:client this) topics))))

(defn create [config]
  (log/debug "Connecting to broker")
  (map->MqttClient {:config config
                    :client (mh/connect (gen-uri-string config)
                                        (gen-paho-options config))}))


(comment


  )

