(ns platform.auth)

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

(defn auth-user? [request]
  (contains? (get-in request [:session :identity]) :auth.user/id))
