(ns platform.api.auth
  (:require [clojure.string :as str]
            [platform.model.auth :as model.auth]
            [songpark.taxonomy.auth]
            [taoensso.timbre :as log]))


(defn signup [{{db :database} :data {data :body} :parameters :as request}]
  (let [{:auth.user/keys [password repeat-password]} data]
    (cond (str/blank? password)
          {:status 400
           :body {:error/message "Password cannot be blank"}}

          (not= password repeat-password)
          {:status 400
           :body {:error/message "Password and repeat password must be the same"}}

          :else
          (let [result (model.auth/signup db data)]
            (if result
              {:status 200
               :body {:result :success}}
              {:status 500
               :body {:error/message "Unable to signup"}})))))


(defn login [{{db :database} :data {data :body} :parameters :as request}]
  (let [user (model.auth/login db data)]
    (if user
      {:status 200
      :session {:identity {:auth.user/id (:auth.user/id user)}}
       :body user}
      {:status 500
       :body {:error/message "Unable to login"}})))

(defn logout [_]
  {:status 200
   :session nil
   :body {:result :success}})

(defn verify-email [{{db :database} :data {data :body} :parameters :as request}]
  (let [result (model.auth/verify-email db data)]
    {:status 200
     :body {:result (if result
                      :success
                      :failure)}}))

(defn forgotten-password [{{db :database} :data {data :body} :parameters :as request}]
  (model.auth/forgotten-password db data)
  {:status 200
   :body {:result :success}})

(defn reset-password [{{db :database} :data {data :body} :parameters :as request}]
  (let [result (model.auth/reset-password db data)]
    {:status 200
     :body {:result (if result
                      :success
                      :failure)}}))
