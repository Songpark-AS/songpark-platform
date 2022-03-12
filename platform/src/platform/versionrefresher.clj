(ns platform.versionrefresher
  (:require [chime.core :as chime]
            [com.stuartsierra.component :as component]
            [platform.util :refer [get-apt-package-version]]
            [songpark.jam.platform.protocol :as proto]
            [taoensso.timbre :as log])
  (:import (java.time Instant Duration)))

;; System component to refresh teleporter-fw version
;; Uses chime to fetch what the latest version of the teleporter firmware is, and stores that information in the database

(defn- set-teleporter-fw-version [db version]
  (proto/write-db db [:teleporter-fw-version] version))

(defn- init-chime-schedule [db]
  (chime/chime-at (chime/periodic-seq (Instant/now)
                                      (Duration/ofMinutes 5))
                  (fn [time]
                    (set-teleporter-fw-version db
                                               (get-apt-package-version "teleporter-fw")))))

(defrecord VersionRefresher [started? config closeable db]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do
        (log/debug "Starting VersionRefresher")
        (assoc this
               :started? true
               :closeable (init-chime-schedule db)))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/debug "Stopping VersionRefresher")
        (.close closeable)
        (assoc this
               :started? false
               :closeable nil)))))

(defn versionrefresher [settings]
  (map->VersionRefresher settings))
