(ns platform.versionrefresher
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [chime.core :as chime]
            [platform.util :refer [get-apt-package-version]]
            [platform.db.store :as store])
  (:import (java.time Instant Duration)))

;; System component to refresh teleporter-fw version
;; Uses chime to fetch what the latest version of the teleporter firmware is, and stores that information in the database

(defn- set-teleporter-fw-version [version]
  (store/wr [:teleporter-fw-version] version))

(defn- init-chime-schedule []
  ;; Don't wait for 5 minutes initially
  (set-teleporter-fw-version (get-apt-package-version "teleporter-fw"))

  (chime/chime-at (-> (chime/periodic-seq (Instant/now) (Duration/ofMinutes 5))
                      rest)
                  (fn [time]
                    (set-teleporter-fw-version (get-apt-package-version "teleporter-fw")))))

(defrecord VersionRefresher [started? config]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do
        (log/debug "Starting VersionRefresher")
        (let [new-this (assoc this
                              :started? true
                              :closeable (init-chime-schedule))]
          new-this))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/debug "Stopping VersionRefresher")
        (.close (:closeable this))
        (let [new-this (assoc this :started? false)]
          new-this)))))
(defn versionrefresher [settings]
  (map->VersionRefresher settings))
