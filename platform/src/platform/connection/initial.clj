(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as sql]
            ))
 
(comment
  (require '[yesql.core :refer [defqueries]])
  (sql/usr-get-all)
  (:e_mail (sql/usr-add<! {:e_mail "bsabla" :descr "blabla" :passwd "lala"}))
  (sql/usr-clear!)
  (sql/tp-activitation! {:tpid 19})
  (sql/tp-deactivitation! {:tpid 19})
  )

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

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
      (basic-return-message "You are client. You wish to connect to tp-id: " (get params "tp-id")
                            ". Total db changes made: " (sql/tp-activitation! {:tpid (parse-int (get params "tp-id"))}))
      (basic-return-message "You are client. You must provide tp-id. BSID: " (get params "BSID")))))