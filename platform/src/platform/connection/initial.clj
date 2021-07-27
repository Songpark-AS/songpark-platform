(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as db]))

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defn- basic-return-message [& items]
  {:status 200
   :body (apply str items)})


(defn tp-init-connect-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (if tpid
      (if (= tpid "all")
        (do 
          (db/tp-set-all-available!)
          (db/tp-set-all-on!)
          {:status 200
           :body (str {:status "success"})})
        (let [rows-updated-on (db/tp-set-on! {:tpid tpid})
            rows-updated-available (db/tp-set-available! {:tpid tpid})]        
        {:status 200
         :body (str "DB queried." 
                    " Rows updated when setting tp-on condition:" rows-updated-on 
                    "  Rows updated when setting tp-available condition:" rows-updated-available)}))
      {:status 400
       :body "ERROR, I need tpid parameter in url"})))

(defn client-init-connect-handler [input]
  (let [nickname (get (:params input) "nickname")]
    (if nickname
      (let [tpid (:unique_id (first (db/tpid-from-nick {:nickname nickname})))]
        (if (and (:available_status (first (db/tp-get-availability {:tpid tpid})))
                 (:on_status (first (db/tp-get-on-status {:tpid tpid}))))
          (do
            (db/tp-set-unavailable! {:tpid tpid})
            {:status 200
             :body (str {:status "success" :mqtt-username "nameynamename" :mqtt-password "super-secret-password!" :tpid tpid})})
          {:status 200
           :body (str {:status "ERROR-tp-unavailable"})}))
      
      {:status 200
       :body (str {:status "ERROR-no-nickname"})})))