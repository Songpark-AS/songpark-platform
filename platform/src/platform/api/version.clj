(ns platform.api.version
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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
