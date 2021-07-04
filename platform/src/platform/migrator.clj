(ns platform.migrator
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [migratus.core :as migratus]
            [taoensso.timbre :as log]))

(defprotocol IMigrator
  (migrate [migrator] [migrator mmap])
  (rollback [migrator mmap]))


(defn get-migration-map [datasource]
  {:store :database
   :migration-table-name "migrations"
   :migration-dir "migrations/"
   :db datasource})

(defrecord MigrationManager [database started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (let [this (assoc this :started? true)]
        (log/info "Starting MigrationManager")
        (migrate this)
        this)))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping MigrationManager")
          (assoc this :started? false))))
  
  IMigrator
  (migrate [this]
    (if-let [ds (get-in database [:db-specs :default :datasource])]
      (let [mmap (get-migration-map ds)]
        (log/info "Starting migrations")
        (migratus/migrate mmap))
      (log/warn "No database found for migrations")))
  (rollback [this mmap]
    (migratus/rollback mmap)))

(defn migration-manager [settings]
  (map->MigrationManager settings))
