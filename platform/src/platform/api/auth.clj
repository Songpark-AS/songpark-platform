(ns platform.api.auth
  (:require [clojure.string :as str]
            [platform.model.auth :as model.auth]
            [songpark.taxonomy.auth]
            [taoensso.timbre :as log]))


(defn whoami [{{db :database} :data identity :identity :as request}]
  (if-let [id (:auth.user/id identity)]
    (if-let [user (model.auth/get-user db id)]
      {:status 200
       :body user}
      (do
        (log/error "Unable to find user" {:auth.user/id id})
        {:status 500
         :body {:error/message "Unable to find any user with that id"}}))
    {:status 200
     :body {:auth.user/status :no-auth}}))

(defn signup [{{db :database} :data {data :body} :parameters :as request}]
  (let [{:auth.user/keys [password repeat-password email]} data]
    (cond (model.auth/user-exists? db email)
          {:status 400
           :body {:error/message "User already exists"
                  :error/key :auth.user/email}}
          (str/blank? password)
          {:status 400
           :body {:error/message "Password cannot be blank"
                  :error/key :auth.user/password}}

          (not= password repeat-password)
          {:status 400
           :body {:error/message "Password and repeat password must be the same"
                  :error/key :auth.user/repeat-password}}

          :else
          (let [result (model.auth/signup db data)]
            (if result
              {:status 200
               :body result}
              {:status 500
               :body {:error/message "Unknown error. Contact the administrator for help."
                      :error/key :auth.user/repeat-password}})))))

(defn login [{{db :database} :data {data :body} :parameters :as request}]
  (let [{:auth.user/keys [email]} data]
    (if (model.auth/user-exists? db email)
      (let [user (model.auth/login db data)]
        (if user
          {:status 200
           :session {:identity {:auth.user/id (:auth.user/id user)}}
           :body user}
          {:status 400
           :body {:error/message "Unable to login. Do you have the right password?"
                  :error/key :auth.user/password}}))
      {:status 400
       :body {:error/message "User does not exist"
              :error/key :auth.user/email}})))

(defn logout [_]
  {:status 200
   :session nil
   :body {:result :success}})

(defn verify-email [{{db :database} :data {data :body} :parameters :as request}]
  (let [result (model.auth/verify-email db data)]
    (if result
      {:status 200
       :body {:result :success}}
      {:status 400
       :body {:error/message "Unable to verify the email. Did you give the correct token?"}})))

(defn send-verify-email [{{db :database} :data {data :body} :parameters :as request}]
  (let [result (model.auth/send-verify-email db data)]
    {:status 200
     :body {:result :success}}))


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
