(ns migrations
  (:require [platform.init :as init]
            [platform.migrator :refer [get-migration-map]]
            [migratus.core :as migratus]))

(defn migratus-config []
  (let [ds (get-in @init/system [:database :db-specs :default :datasource])
        mmap (get-migration-map ds)]
    (assert (some? ds) "The system needs to be initialized in order to work with migrations")
    mmap))

(comment

  (migratus/create (migratus-config) "view-examination-expected-results-added-locale")
  (migratus/create (migratus-config) "view-student-history-added-locale")
  (migratus/create (migratus-config) "view-assignment-student-locale")
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))
  )
