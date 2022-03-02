(ns platform.api.jam
  (:require [clj-uuid :as uuid]
            [taoensso.timbre :as log]
            [songpark.jam.platform :as jam.platform]
            [songpark.mqtt :as mqtt]
            [songpark.mqtt.util :refer [teleporter-topic]]
            [songpark.taxonomy.teleporter]
            [songpark.taxonomy.jam]
            [platform.api :refer [send-message!]]
            [platform.db.store :as db]))


;; Does not check whether or not the jam actually
;; exists, so it will return 200 no matter what. It
;; will however delete the jam if it exists.
(defn stop [{:keys [data parameters]}]
  (let [jam-id (-> parameters :body :jam/id)
        {:keys [mqtt-client jam-manager]} data]
    (jam.platform/stop jam-manager jam-id)
    {:status 200
     :body {:jam/id jam-id
            :jam/status false}}))

(defn ask [{{jam-manager :jam-manager} :data :keys [parameters] :as _request}]
  (let [tp-id (-> parameters :body :teleporter/id)]
    (jam.platform/ask jam-manager tp-id)
    {:status 200
     :body {:jam.ask/status true}}))

(defn obviate [{{jam-manager :jam-manager} :data :keys [parameters] :as _request}]
  (let [tp-id (-> parameters :body :teleporter/id)]
    (jam.platform/obviate jam-manager tp-id)
    {:status 200
     :body {:jam.ask/status false}}))



