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

  (migratus/create (migratus-config) "add-profile")
  (migratus/migrate (migratus-config))
  (migratus/rollback (migratus-config))
  )


name text,
location text,
bio text,
image_url text,
-- decide this in code
-- 0 is unknown
pronoun integer not null default 0
