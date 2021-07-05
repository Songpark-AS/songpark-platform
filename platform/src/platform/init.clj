(ns platform.init
  (:require [platform.aws :as aws]
            [platform.config :refer [config]]
            [platform.database :as database]

            [platform.http.server :as http.server]
            [platform.logger :as logger]
            [platform.migrator :as migrator]
            [com.stuartsierra.component :as component]
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
        store (carmine.ring/carmine-store (:carmine config))]
    (apply component/system-map
           (into [:logger logger
                  :aws (aws/aws-client (:aws config))
                  ;; NB! Commented out as per discussion here: https://inonit.slack.com/archives/CCBJTBN66/p1624464552409300
                  ;;:google (google/google-client (:google config))
                  :migration-manager (component/using (migrator/migration-manager (:migration config))
                                                      [:database])
                  :config core-config
                  :http-server (component/using (http.server/http-server
                                                 (merge (:http config)
                                                        {:store store}))
                                                [:database :aws #_:google])
                  :database (database/database {:db-specs {:default {}}
                                                :ds-specs datasource})]
                 extra-components))))

(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (log/info "Shutting songpark platform down")
    (try (component/stop @system)
         (catch Throwable t
           (log/error "Tried to shut down songpark platform. Got" t)))
    (log/info "songpark platform is now shut down")
    (reset! system nil)))

(defn init [& extra-components]
  (if @system
    (log/info "songpark platform already running")
    (do
      (log/info "Starting songpark platform up")
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
