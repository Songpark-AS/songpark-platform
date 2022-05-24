(ns platform.http.server
  (:require [platform.config :refer [config]]
            [platform.http.middleware :refer [unauthorized-handler]]
            [platform.http.route :as route]
            [buddy.auth.backends :as backends]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [taoensso.timbre :as log]))

(defn create-server [settings middleware-data]
  (jetty/run-jetty (route/get-routes middleware-data) settings))

(defn stop-server [server]
  (.stop server))


;; db is a protococol based in-memory db
;; datababase is postgres
(defrecord HTTPServer [started? server-settings server store mqtt-client db database jam-manager]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting HTTPServer")
          (let [session-backend (backends/session {:unauthorized-handler unauthorized-handler})
                server (create-server server-settings
                                      {:store store
                                       :authz.backend/session session-backend
                                       :http/cookies (:http/cookies server-settings)
                                       :songpark/data {:mqtt-client mqtt-client
                                                       :db db
                                                       :database database
                                                       :jam-manager jam-manager}})]
            (assoc this
                   :started? true
                   :server server)))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping HTTPServer")
          (stop-server server)
          (assoc this
                 :started? false
                 :server nil)))))

(defn http-server [settings]
  (map->HTTPServer settings))
