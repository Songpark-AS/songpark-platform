(ns platform.init
  (:require [com.stuartsierra.component :as component]
            [platform.config :refer [config]]
            [platform.database :as database]
            [platform.http.server :as http.server]
            [platform.migrator :as migrator]
            [platform.mqtt.handler.jam]
            [platform.mqtt.handler.teleporter]
            [platform.logger :as logger]
            [platform.room :as room]
            [platform.scheduler :as scheduler]
            [platform.versionrefresher :as versionrefresher]
            [songpark.jam.platform :refer [mem-db jam-manager]]
            [songpark.mqtt :as mqtt]
            [taoensso.carmine]
            [taoensso.carmine.ring :as carmine.ring]
            [taoensso.timbre :as log]))

(defn- system-map [extra-components]
  (let [;; logger and config are started this way so that we can ensure
        ;; things are logged as we want and that the config is loaded
        ;; for all the other modules
        core-config (component/start (platform.config/config-manager {}))
        logger (component/start (logger/logger (:logger config)))
        datasource (get-in config [:database :datasource])
        db (mem-db {:teleporter {}
                    :jam {}
                    :waiting {}})
        ;; start mqtt client first in order to let it connect
        mqtt-client (component/start (mqtt/mqtt-client (assoc-in (:mqtt config) [:config :id] "platform")))
        store (carmine.ring/carmine-store (:carmine config))]
    (apply component/system-map
           (into [:logger logger
                  :config core-config
                  :migration-manager (component/using (migrator/migration-manager (:migration config))
                                                      [:database])
                  :database (database/database {:db-specs {:default {}}
                                                :ds-specs datasource})
                  :versionrefresher (versionrefresher/versionrefresher
                                     (assoc (:versionrefresher config)
                                            :db db))
                  :http-server (component/using (http.server/http-server (merge (:http config)
                                                                                {:db db
                                                                                 :store store}))
                                                [:mqtt-client :jam-manager :database :roomdb])
                  :jam-manager (component/using (jam-manager {:db db})
                                                [:mqtt-client])
                  :roomdb (component/using (room/room-db {})
                                           [:mqtt-client :jam-manager :database])
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

      (let [mqtt-client (:mqtt-client @system)
            jam-manager (:jam-manager @system)
            database (:database @system)]
        (mqtt/subscribe mqtt-client "jam" 2)
        (mqtt/add-injection mqtt-client :jam-manager jam-manager)
        (mqtt/add-injection mqtt-client :database database))

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
