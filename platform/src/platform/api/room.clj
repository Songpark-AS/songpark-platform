(ns platform.api.room
  (:require [platform.model.room :as model.room]
            [songpark.taxonomy.room]
            [taoensso.timbre :as log]))

(defn get-rooms [{{db :database} :data
                  {user-id :auth.user/id} :identity
                  :as request}]
  (let [result (model.room/get-rooms db user-id)]
    {:status 200
     :body result}))


(defn save-room [{{db :database} :data
                  {data :body} :parameters
                  {user-id :auth.user/id} :identity
                  :as request}]
  (if-let [name-exists? (model.room/name-exists? db (:room/name data))]
    {:status 400
     :body {:error/message "Name already exists"
            :error/key :room/name}}
    (let [result (model.room/save-room db user-id data)]
      {:status 200
       :body result})))

(defn update-room [{{db :database} :data
                    {data :body} :parameters
                    {user-id :auth.user/id} :identity
                    :as request}]
  (let [id-exists? (model.room/id-exists? db (:room/id data))
        name-exists? (model.room/name-exists? db (:room/id data) (:room/name data))
        owner? (model.room/is-owner? db (:room/id data) user-id)]
    (cond (not id-exists?)
          {:status 400
           :body {:error/message "ID does not exist"
                  :error/key :room/name}}
          name-exists?
          {:status 400
           :body {:error/message "Name already exists"
                  :error/key :room/name}}
          (not owner?)
          {:status 400
           :body {:error/message "You do not own this room"
                  :error/key :room/name}}
          :else
          (let [result (model.room/update-room db data)]
            {:status 200
             :body result}))))
