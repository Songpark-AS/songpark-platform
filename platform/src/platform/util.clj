(ns platform.util
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn kw->str [x]
  (if (keyword? x)
    (subs (str x) 1)
    x))

(defn str->kw [x]
  (if (string? x)
    (keyword x)
    x))

(defn as->provider
  "Takes a IdP service name and a unique ID identifying a user
  at this provider and return the value of the provider for this
  identity (e.g: :feide/224adaaa28)"
  [idp-name id]
  (assert (string? idp-name) "The idp-name must be string")
  (assert (string? id) "The id must be a string")

  (keyword idp-name id))

(def http-ok {:result :success})

(defn get-apt-package-version
  "Returns the latest version number available based on an apt package name.
  Requires apt to be installed"
  [package-name]
  (sh "bash" "-c" "apt update")
  (let [apt-show-command (str "apt-cache show " package-name)
        apt-string (:out (sh "bash" "-c" apt-show-command))
        version-line (first (filter (fn [line] (re-matches #"Version.*" line))
                                    (str/split apt-string #"\n")))]
    (last (str/split (or version-line "Unknown") #": "))))
