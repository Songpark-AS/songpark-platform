(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]))

(defn- basic-return-message [& items] 
  {:status 200
   :body (apply str items)})

(defn tp-handler [input]
  (let [params (:params input)] 
  (if (get params "tp-id")
    (basic-return-message "You are tp. Your tp-id is: " (get params "tp-id"))
    (basic-return-message "You are tp. You must provide tp-id"))))

(defn client-handler [input]
  (let [params (:params input)]
    (if (get params "tp-id")
      (basic-return-message "You are client. You wish to connect to tp-id: " (get params "tp-id"))
      (basic-return-message "You are client. You must provide tp-id"))))