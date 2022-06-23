(ns platform.api.pairing
  (:require [platform.model.pairing :as model.pairing]
            [platform.util :refer [id->uuid
                                   serial->uuid]]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic]]
            [songpark.taxonomy.pairing]
            [taoensso.timbre :as log]))

(defn get-pairs [{{db :database} :data
                  {user-id :auth.user/id} :identity
                  :as request}]
  {:status 200
   :body (model.pairing/get-pairs db user-id)})

(defn pair [{{db :database
              mqtt-client :mqtt-client} :data
             {data :body} :parameters
             {user-id :auth.user/id} :identity
             :as request}]
  (let [serial (:teleporter/serial data)
        serial-exists? (model.pairing/serial-exists? db serial)
        already-paired? (model.pairing/already-paired? db user-id serial)]
    (cond (not serial-exists?)
          {:status 400
           :body {:error/message "Serial does not exist"
                  :error/key :teleporter/serial}}

          already-paired?
          {:status 400
           :body {:error/message "Teleporter is already paired"
                  :error/key :teleporter/serial}}

          :else
          (let [teleporter-id (-> data
                                  :teleporter/serial
                                  (serial->uuid))
                topic (teleporter-topic teleporter-id)]
            (mqtt/publish mqtt-client
                          topic
                          {:message/type :pairing/pair
                           :teleporter/id teleporter-id
                           :auth.user/channel (id->uuid user-id)
                           :auth.user/id user-id})
            {:status 200
             :body {:status :success}}))))

(defn unpair [{{db :database} :data
               {data :body} :parameters
               {user-id :auth.user/id} :identity
               :as request}]
  )
