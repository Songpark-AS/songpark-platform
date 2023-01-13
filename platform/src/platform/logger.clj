(ns platform.logger
  (:require [com.stuartsierra.component :as component]
            [me.raynes.fs :as fs]
            [platform.config :refer [config]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.3rd-party.rotor :refer [rotor-appender]]
            [taoensso.timbre.appenders.3rd-party.sentry :refer [sentry-appender]]))


(defrecord Logger [started? sentry-settings]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [level (get-in config [:logger :level] :info)]
        (fs/mkdir "logs")
        ;; set level of logging
        (log/set-level! level)

        (log/merge-config! {:appenders (merge
                                        {:rotor (rotor-appender {:path "logs/songpark.log"
                                                                 :backlog 100})}
                                        (if (:log? sentry-settings)
                                          {:raven (sentry-appender (:dsn sentry-settings))}))})
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
