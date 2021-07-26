(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as sql]))

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defn- basic-return-message [& items]
  {:status 200
   :body (apply str items)})

(defn- format-client-response-map [status username password]
  (format "{:status \"%s\" :mqtt-username \"%s\" :mqtt-password \"%s\"}" status username password))

(defn tp-init-connect-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (if tpid
      (let [rows-updated-on (sql/tp-set-on! {:tpid tpid})
            rows-updated-available (sql/tp-set-available! {:tpid tpid})]        
        {:status 200
         :body (str "DB queried." 
                    " Rows updated when setting tp-on condition:" rows-updated-on 
                    "  Rows updated when setting tp-available condition:" rows-updated-available)})
      {:status 400
       :body "ERROR, I need tpid parameter in url"})))

(defn client-init-connect-handler [input]
  (let [nickname (get (:params input) "nickname")]
    (if nickname
      (let [tpid (:unique_id (first (sql/tpid-from-nick {:nickname nickname})))]
        (if (and (:available_status (first (sql/tp-get-availability {:tpid tpid})))
                (:on_status (first (sql/tp-get-on-status {:tpid tpid}))))
        {:status 200
         :body (format-client-response-map "success" "username" "password")}
        {:status 200
         :body (format-client-response-map "ERROR-tp-unavailable" nil nil)}))
      
      {:status 200
       :body (format-client-response-map "ERROR-no-nickname" nil nil)})))