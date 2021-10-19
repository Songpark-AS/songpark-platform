(ns platform.auth
  (:require [buddy.auth.backends :as backends]))

(defn get-credentials
  ([request]
   (get-in request [:session :identity]))
  ([request ks]
   (select-keys (get-in request [:session :identity]) ks)))

(defn merge-credentials
  ([request data]
   (merge data
          (get-in request [:session :identity])))
  ([request data ks]
   (let [identity (get-in request [:session :identity])]
     (merge data
            (select-keys identity ks)))))

(defn teacher? [request]
  (contains? (get-in request [:session :identity]) :teacher/id))

(defn student? [request]
  (contains? (get-in request [:session :identity]) :student/id))

(defn auth-user? [request]
  (contains? (get-in request [:session :identity]) :auth.user/id))


;; Super basic authentication

^:private (def super-secret-creds (atom {:username "songpark"
                                         :password "N0xLE1aD3VhtHtEaK7CyTqOufm3UNtMGSuWaLtjW"}))

(defn my-authfn
  [request authdata]
  (let [username (:username authdata)
        password (:password authdata)]
    (and (= username (:username @super-secret-creds)) (= password (:password @super-secret-creds)))
    ))

(def backend (backends/basic {:realm "SongparkApi"
                              :authfn my-authfn}))
