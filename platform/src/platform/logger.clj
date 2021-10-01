(ns platform.logger
  (:require [com.stuartsierra.component :as component]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :refer [rotor-appender]]
            [taoensso.timbre.appenders.3rd-party.sentry :refer [sentry-appender]]
            [vlaaad.reveal.ext :as rx]))

(defn reaveal-tap-fn [data]
  (tap> (rx/as data
               (rx/raw-string
                (format "[%1$tH:%1$tM:%1$tS.%1$tL %2$s:%3$s]: %4$s"
                        (:instant data)
                        (:?ns-str data)
                        (:?line data)
                        @(:msg_ data))
                {:fill ({:info :symbol
                         :report :symbol
                         :warn "#db8618"
                         :error :error
                         :fatal :error}
                        (:level data)
                        :util)}))))

(defrecord Logger [started? sentry-settings reveal?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (fs/mkdir "logs")
          (log/merge-config! {:appenders (merge
                                          {:rotor (rotor-appender {:path "logs/songpark.log"
                                                                   :backlog 100})}
                                          (if (:log? sentry-settings)
                                            {:raven (sentry-appender (:dsn sentry-settings))})
                                          (if reveal?
                                            {:println {:enabled? false}
                                             :reveal {:enabled? true
                                                      :fn reveal-tap-fn}}))})
          (log/info "Starting Logger")
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Logger")
          (assoc this
                 :started? false)))))


(defn logger [settings]
  (map->Logger settings))



(commment
 (merge {:a 1} {:b 2} {:c 3})

 )
