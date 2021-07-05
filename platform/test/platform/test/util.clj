(ns platform.test.util
  (:require [platform.aws :as aws]
            [platform.database :as database]

            [platform.migrator :as migrator]
            [platform.model.auth :as model.auth]
            [buddy.core.codecs.base64 :refer [encode decode]]
            [cprop.source :refer [from-system-props from-env]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [migratus.core :as migratus]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t])
  (:import [java.nio.file Files]))

(log/set-level! :warn)

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      (last vs))))

(defn b64-encode [path]
  (encode (Files/readAllBytes (.toPath (io/file (io/resource path))))))

(defn- read-config []
  (-> (io/resource "config.edn")
      (slurp)
      (edn/read-string)))


(defn start-db []
  (let [db-config (deep-merge
                   {:datasource {:default {:maximum-pool-size 3
                                           :adapter "postgresql"
                                           :username "postgres"
                                           :password "postgres"
                                           :database-name "songparktest"
                                           :server-name "localhost"
                                           :port-number 5432}}}
                   (select-keys (:database (from-env)) [:datasource]))
        datasource (:datasource db-config)]
    (log/merge-config! {:level :info})
    (log/info "Starting database for testing")
    (component/start (database/database {:db-specs {:default {}}
                                         :ds-specs datasource}))))

(defn stop-db [db]
  (log/info "Stopping database for testing")
  (log/merge-config! {:level :debug
                      :ns-blacklist ["org.eclipse.jetty.*"
                                     "io.grpc.netty.shaded.io.netty.*"
                                     "org.opensaml.*"]})
  (component/stop db))

(defn start-aws []
  (log/info "Starting AWS client for testing")
  (component/start (aws/aws-client (:aws (read-config)))))

(defn stop-aws [aws]
  (log/info "Stopping AWS client for testing")
  (component/stop aws))

(defn cleanup-db!
  "Clean up database"
  [db]
  (let [ds (get-in db [:db-specs :default :datasource])
        _ (prn :ds-db!!! ds db)
        migration-ids (try
                        ;; if the database is entirely new, there will
                        ;; be no migrations table
                        (->> {:select [:id]
                              :from [:migrations]
                              :order-by [[:id :desc]]}
                             (db/query db)
                             (mapv :id))
                        (catch Exception _
                          nil))]
    (prn :migids!!!!! migration-ids)
    (when migration-ids
      (apply migratus/down (migrator/get-migration-map ds) migration-ids))))


(defn run-migrations! [db]
  (component/start (migrator/migration-manager {:database db})))


(defn prepare-db! [db]
  (cleanup-db! db)
  (run-migrations! db))


(defn seed-db! [db]
  (log/info "Seeding the database")
  ;; skip spam
  (log/set-level! :warn)
  (prepare-db! db)

  ;; add superuser
  (model.auth/add-user! db #:auth.user{:email "test@songpark.no" :first-name "Test" :last-name "McTest" :password "testme" :active? true})
  (log/set-level! :info))
