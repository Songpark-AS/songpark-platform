(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]))

(comment
  (require '[yesql.core :refer [defqueries]])
  (defn parse-int [s]
    (Integer. (re-find  #"\d+" s)))

  (def db-spec {:classname "org.postgresql.Driver"
                :subprotocol "postgresql"
                :subname "//localhost:5432/songpark"
                :user "postgres"
                :password "postgres"})

  (defqueries "platform/connection/queries/test.sql" {:connection db-spec})
  (usr-get-all)
  (:e_mail (usr-add<! {:e_mail "bsabla" :descr "blabla" :passwd "lala"}))
  (usr-clear!)
  (tp-activitation! {:tpid 19})
  (tp-deactivitation! {:tpid 19}))


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
                            ". Total db changes made: " (tp-activitation! {:tpid (parse-int (get params "tp-id"))}))
      (basic-return-message "You are client. You must provide tp-id. BSID: " (get params "BSID")))))