(ns platform.init
  (:require [platform.config :refer [config]]
            [platform.db.store :refer [rd wr]]
            [platform.http.server :as http.server]
            [platform.logger :as logger]
            [platform.message :as message]
            [platform.mqtt :as mqtt]
            [platform.api :as api]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (platform.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        mqtt-config (:mqtt config)]
    (apply component/system-map
           (into [:logger logger
                  :config core-config
                  :http-server (http.server/http-server (:http config))
                  :mqtt-manager (mqtt/mqtt-manager (:mqtt config))
                  :message-service (component/using (message/message-service
                                                     {:injection-ks [:mqtt]})
                                                    {:mqtt :mqtt-manager})
                  :api-manager (component/using (api/api-manager {:injection-ks [:message-service]})
                                                [:message-service])]
                 extra-components))))

(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (log/info "Shutting down Songpark Platform")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down Songpark Platform. Got" t)))
    (log/info "Songpark Platform is now shut down")
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
