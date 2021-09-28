(ns platform.api
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [platform.message.handler.incoming :as handler.incoming]
            [platform.message.handler.outgoing :as handler.outgoing]))

(defonce ^:private store (atom nil))

(defn send-message! [msg]
  (let [api @store
        injections (-> api
                       (select-keys (:injection-ks api))
                       (assoc :api api))]
    (.send-message! (:messenger injections) msg)))

(defrecord ApiManager [injection-ks started? mqtt]
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

  )
