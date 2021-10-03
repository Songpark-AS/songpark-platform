(ns platform.api
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defonce ^:private store (atom nil))

(defn send-message! [msg]
  (let [api @store
        injections (-> api
                       (select-keys (:injection-ks api))
                       (assoc :api api))]
    (.send-message! (:message-service injections) (merge msg injections))))

(defrecord ApiManager [injection-ks started? message-service mqtt-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting ApiManager")
          (let [new-this (assoc this
                                :started? true)]
            (reset! store new-this)
            new-this))))
  
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping ApiManager")
          (let [new-this (assoc this
                                :started? false)]
            (reset! store nil)
            new-this)))))

(defn api-manager [settings]
  (map->ApiManager settings))


(comment
  (-> (select-keys @store (:injection-ks @store)))
  (send-message! {:message/type :platform.cmd/subscribe
                  :message/meta {:mqtt/topics {"4577bed8-08b7-54cf-ae89-2061ef434b2f" 0}}})
  )



