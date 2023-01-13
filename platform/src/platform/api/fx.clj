(ns platform.api.fx
  (:require [platform.model.fx :as model.fx]
            [songpark.taxonomy.fx]
            [taoensso.timbre :as log]))



(defn delete-preset [{{db :database} :data
                      {user-id :auth.user/id} :identity
                      {data :body} :parameters
                      :as request}]
  (if-let [result (model.fx/delete-preset db user-id data)]
    {:status 200
     :body data}
    {:status 400
     :body {:error/message "Unable to delete the preset"}}))

(defn save-preset [{{db :database} :data
                    {user-id :auth.user/id} :identity
                    {data :body} :parameters
                    :as request}]
  (if-let [result (model.fx/save-preset db user-id data)]
    {:status 200
     :body result}
    {:status 400
     :body {:error/message "Unable to save the preset"}}))

(defn get-presets [{{db :database} :data
                    {user-id :auth.user/id} :identity
                    :as request}]
  (if-let [result (model.fx/presets db user-id)]
    {:status 200
     :body result}
    {:status 400
     :body {:error/message "Unable to get the presets"}}))

(defn update-preset [{{db :database} :data
                      {user-id :auth.user/id} :identity
                      {data :body} :parameters
                      :as request}]
  (if-let [result (model.fx/update-preset db user-id data)]
    {:status 200
     :body result}
    {:status 200
     :body {:error/message "Unable to update the preset"}}))
