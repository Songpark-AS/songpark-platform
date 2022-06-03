(ns platform.api.profile
  (:require [platform.model.profile :as model.profile]
            [songpark.taxonomy.profile]
            [taoensso.timbre :as log]))


(defn get-profile [{{db :database} :data
                    {user-id :auth.user/id} :identity
                    :as request}]
  (let [result (model.profile/get-profile db user-id)]
    {:status 200
     :body result}))

(defn save-profile [{{db :database} :data
                     {data :body} :parameters
                     {user-id :auth.user/id} :identity
                     :as request}]
  (if-let [exists? (model.profile/name-exists? db user-id (:profile/name data))]
    {:status 400
     :body {:error/message "Name already exists"
            :error/key :profile/name}}
    (let [result (model.profile/save-profile db user-id data)]
      (if result
        {:status 200
         :body (model.profile/get-profile db user-id)}
        {:status 500
         :body {:error/message "Unable to save profile. Please contact the administrator"
                :error/key :profile/location}}))))

(defn get-pronouns [{{db :database} :data
                     :as request}]
  (let [result (model.profile/get-pronouns db)]
    {:status 200
     :body result}))
