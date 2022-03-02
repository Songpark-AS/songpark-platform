(ns platform.init
  (:require [com.stuartsierra.component :as component]
            [platform.config :refer [config]]
            [platform.http.server :as http.server]
            [platform.logger :as logger]
            [platform.mqtt.handler.teleporter]
            [platform.scheduler :as scheduler]
            [platform.versionrefresher :as versionrefresher]
            [songpark.jam.platform :refer [mem-db jam-manager]]
            [songpark.mqtt :as mqtt]
            [taoensso.timbre :as log]))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (platform.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        db (mem-db {:teleporter {}
                    :jam {}
                    :waiting {}})
        ;; start mqtt client first in order to let it connect
        mqtt-client (component/start (mqtt/mqtt-client (assoc-in (:mqtt config) [:config :id] "platform")))]
    (apply component/system-map
           (into [:logger logger
                  :config core-config
                  :versionrefresher  (versionrefresher/versionrefresher (:versionrefresher config))
                  :http-server (component/using (http.server/http-server (merge (:http config)
                                                                                {:db db}))
                                                [:mqtt-client :jam-manager])
                  :jam-manager (component/using (jam-manager {:db db})
                                                [:mqtt-client])
                  :scheduler (component/using (scheduler/scheduler (:scheduler config))
                                              [:jam-manager])
                  :mqtt-client mqtt-client]
                 extra-components))))

(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (log/info "Shutting down Songpark Platform")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down Songpark Platform. Got" t)))
    (log/debug "Songpark Platform is now shut down")
    (reset! system nil)))

(defn init [& extra-components]
  (if @system
    (log/info "Songpark Platform already running")
    (do
      (log/info "Starting Songpark Platform")
      ;; start the system
      (reset! system (component/start (system-map extra-components)))

      ;; log uncaught exceptions in threads
      (Thread/setDefaultUncaughtExceptionHandler
       (reify Thread$UncaughtExceptionHandler
         (uncaughtException [_ thread ex]
           (log/error {:what      :uncaught-exception
                       :exception ex
                       :where     (str "Uncaught exception on" (.getName thread))}))))

      ;; add shutdown hook
      (.addShutdownHook
       (Runtime/getRuntime)
       (proxy [Thread] []
         (run []
           (stop)))))))
