(ns platform.api.auth
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]))


(defn login [{{db :database} :data {{:auth.user/keys [email password]} :body} :parameters}]
  (let [check? (model.auth/check-password db email password)
        user (model.auth/user-by-email db email)]
    (if check?
      {:status 200
       :body user
       :session {:identity {:auth.user/id (:auth.user/id user)
                            :authz/super true}}}
      {:status 401
       :body {:error/message "Unable to login with these user credentials"}})))

(defn logout [request]
  {:status 200
   :body ""
   :session nil})

(defn user [{{db :database} :data {credentials :body} :parameters session :session}]
  (if-let [user-id (get-in session [:identity :auth.user/id])]
    {:status 200
     :body (model.auth/user-by-id db user-id)}
    {:status 400
     :body {:error/message (if (nil? session)
                             "Not logged in"
                             "Unable to find user by session")}}))
