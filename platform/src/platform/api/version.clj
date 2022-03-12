(ns platform.api.version
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [songpark.jam.platform.protocol :as proto]
            [taoensso.timbre :as log]))


(defn get-version [_request]
  (try
    {:status 200
     :body {:version (str/trim (slurp (io/resource "VERSION")))
            :sha (str/trim (slurp (io/resource "VERSION.git")))}}
    (catch Exception e
      (log/warn "No version found in resources")
      {:status 200
       :body {:version "Unknown"
              :sha "Unknown"}})))

(defn get-latest-available-version [{{db :db} :data :as _request}]
  (try
    {:status 200
     :body {:version (str (proto/read-db db [:teleporter-fw-version]))}}
    (catch Exception e
      (log/error ::get-latest-available-version "Exception" e)
      {:status 500})))
