(ns migrations
  (:require [platform.init :as init]
            [platform.migrator :refer [get-migration-map]]
            [migratus.core :as migratus]))

(defn migratus-config []
  (let [ezdb (get-in @init/system [:database])
        ds (get-in @init/system [:database :db-specs :default :datasource])
        mmap (get-migration-map ezdb ds)]
    (assert (some? ds) "The system needs to be initialized in order to work with migrations")
    mmap))

(comment

  (migratus/create (migratus-config) "update-auth-user-with-verified-email-token")
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))
  )
