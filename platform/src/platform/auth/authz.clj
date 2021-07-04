(ns platform.auth.authz
  (:require [buddy.auth :refer [throw-unauthorized]]
            [clojure.set :as set]))


(defn add-namespaces
  "Add namespaces to the roles"
  [roles]
  (into roles
        (->> roles
             (map (comp keyword namespace))
             (remove nil?)
             (into #{}))))

(defn throw-exception [super credentials roles]
  (throw-unauthorized {:message "Credentials not accepted"
                       :type :auth/not-authorized
                       :roles roles
                       :credentials credentials
                       :super super}))

;; allow for :foo to cover for :foo, :foo/bar, :foo/baz, etc
(defn -allow?
  "Credentials need to contain something in roles. If credentials holds anything in super it automatically passes."
  [super credentials roles]
  (assert (or (boolean? super) (nil? super)) "super needs to be either a boolean or nil")
  (assert (every? keyword? credentials) "credentials can only contain keywords")
  (assert (every? keyword? roles) "roles can only contain keywords")

  (if (true? super)
    true
    (let [expanded-roles (add-namespaces roles)]
      (if-not (empty? (set/intersection credentials expanded-roles))
        true
        (throw-exception super credentials roles)))))

(defprotocol ICredentials
  (allow? [user roles]))

(extend-protocol ICredentials
  clojure.lang.PersistentArrayMap
  (allow? [user roles]
    (-allow? (:authz/super user) (:authz/credentials user) roles))
  nil
  (allow? [user roles]
    (throw-exception (:authz/super user) (:authz/credentials user) roles)))


