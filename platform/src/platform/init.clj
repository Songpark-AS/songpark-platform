(ns platform.init
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [platform.config :refer [config]]
            [platform.db.store :refer [rd wr]]
            [platform.http.server :as http.server]
            [platform.logger :as logger]
            [platform.message :as message]
            [platform.mqtt :as mqtt]
            [platform.versionrefresher :as versionrefresher]
            [platform.api :as api]))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (platform.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        versionrefresher (component/start (versionrefresher/versionrefresher {}))
        mqtt-config (:mqtt config)]
    (apply component/system-map
           (into [:logger logger
                  :config core-config
                  :versionrefresher versionrefresher
                  :http-server (http.server/http-server (:http config))
                  :message-service (message/message-service (:message config))
                  :mqtt-manager (component/using
                                 (mqtt/mqtt-manager (merge (:mqtt config)
                                                           {:injection-ks [:message-service]}))
                                 [:message-service])                  
                  :api-manager (component/using (api/api-manager
                                                 {:injection-ks [:message-service :mqtt-manager]})
                                                [:message-service :mqtt-manager])]
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
