(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]))


(defn tp-handler [input]
  (let [sub (:params input)] 
  (if (get sub "tp-id")
    {:status 200
     :body (str "You are tp. Your tp-id is: " (get sub "tp-id"))}
    {:status 200
     :body (str "You are tp. You must provide tp-id")}))
)
(defn client-handler [input]
  (let [sub (:params input)]
    (if (get sub "tp-id")
    {:status 200
     :body (str "You are client. You wish to connect to tp-id: " (get sub "tp-id"))}
    {:status 200
     :body (str "You are client. You must provide tp-id")})))