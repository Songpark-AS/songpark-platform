(ns platform.api.pairing
  (:require [platform.model.app :as model.app]
            [platform.model.pairing :as model.pairing]
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
                           :auth.user/id user-id})
            {:status 200
             :body {:status :success}}))))

(defn paired [{{db :database
                memdb :db
                mqtt-client :mqtt-client} :data
               {{user-id :auth.user/id
                 teleporter-id :teleporter/id
                 :as data} :body} :parameters
               :as request}]
  (let [result (model.pairing/pair db user-id teleporter-id)
        teleporters (model.app/get-teleporters db memdb user-id)]
    (mqtt/publish mqtt-client
                  (id->uuid user-id)
                  {:message/type :pairing/paired
                   ;; this is mostly to not break the old code base
                   :teleporter/teleporters teleporters}))
  {:status 200
   :body {:status :success}})

(defn unpair [{{db :database
                mqtt-client :mqtt-client} :data
               {data :body} :parameters
               {user-id :auth.user/id} :identity
               :as request}]
  (let [{teleporter-id :teleporter/id} data
        result (model.pairing/unpair db user-id)]
    ;; we deleted a pairing
    (when-not (empty? result)
      (let [topic (teleporter-topic teleporter-id)]
        (mqtt/publish mqtt-client
                      topic
                      {:message/type :pairing/unpair
                       :teleporter/id teleporter-id
                       :auth.user/id user-id})))
    {:status 200
     :body {:result :success}}))
