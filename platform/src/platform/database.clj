(ns platform.database
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [hikari-cp.core :as hikari-cp]
            [taoensso.timbre :as log])
  (:import [ez_database.core EzDatabase]
           [java.sql PreparedStatement]
           [org.postgresql.util PGobject]))


;; post-query for getting only the first value
;; use when you know you're only going to get back one result
(defmethod db/post-query [:post :one] [_ ks _ values]
  (first values))

(defn primitive-array? [o]
  (or (nil? o)
      (.. (type o) isArray)))

(defmulti array-value primitive-array?)
(defmethod array-value true [x]
  (into [] x))
(defmethod array-value :default [x]
  x)

;; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _2 _3]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "jsonb" (json/parse-string value true)
        "json" (json/parse-string value true)
        value)))
  org.postgresql.jdbc.PgArray
  (result-set-read-column [pgobj metadata i]
    (mapv array-value (vec (.getArray pgobj))))
  java.sql.Timestamp
  (result-set-read-column [v _ _]
    (.toInstant v)))

;; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
(extend-protocol jdbc/ISQLValue
  ;; java.util.Date
  ;; (sql-value [v]
  ;;   (t.coerce/to-sql-time v))
  java.time.Instant
  (sql-value [v]
    (java.sql.Timestamp/from v)))

(extend-protocol jdbc/ISQLParameter
  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s ^long i]
    (let [conn (.getConnection s)
          meta (.getParameterMetaData s)
          type-name (.getParameterTypeName meta i)]
      (if-let [elem-type (when type-name (second (re-find #"^_(.*)" type-name)))]
        (.setObject s i (.createArrayOf conn elem-type (to-array v)))
        (.setObject s i v)))))


(defn- get-datasource
  "HikaruCP based connection pool"
  [db-spec datasource]
  {:datasource (hikari-cp/make-datasource datasource)})


(extend-type EzDatabase
  component/Lifecycle
  (start [this]
    (let [{:keys [db-specs ds-specs]} this]
      (if (get-in db-specs [:default :datasource])
        this
        (do (log/info "Starting database")
            (let [db-specs (->> (keys db-specs)
                                (map
                                 (fn [key]
                                   (let [db-spec (get db-specs key)
                                         ds-spec (get ds-specs key)]
                                     [key (get-datasource db-spec ds-spec)])))
                                (into {}))]
              (assoc this
                     :db-specs db-specs
                     :ds-specs ds-specs))))))
  (stop [this]
    (let [db-specs (:db-specs this)]
      (if-not (get-in db-specs [:default :datasource])
        this
        (do
          (log/info "Stopping database")
          (doseq [[key db-spec] db-specs]
            (hikari-cp/close-datasource (:datasource db-spec))
            (log/info "Closed datasource for" key))
          (assoc this
                 :db-specs (into
                            {} (map (fn [[key db-spec]]
                                      [key (dissoc db-spec :datasource)])
                                    db-specs))))))))

(defn database
  ([settings]
   (db/map->EzDatabase settings))
  ([dev? db-specs]
   (db/map->EzDatabase {:dev? dev? :db-specs db-specs :ds-specs {}}))
  ([dev? db-specs ds-specs]
   (db/map->EzDatabase {:dev? dev? :db-specs db-specs :ds-specs ds-specs})))
