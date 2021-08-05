(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as db]))


(defn tp-init-connect-handler [input]
  (let [parameters (:params input)
        tpid (get parameters "tpid")
        uuid (.toString (java.util.UUID/randomUUID))]
    (if tpid
      (do (db/tp-set-on! {:tpid tpid})
          (db/tp-set-available! {:tpid tpid :uuid uuid})
          {:status 200
           :body (str {:status "success"
                       :status-desc (str "Successfully updated the database. Teleporter:" tpid " is now on and available. It has uuid: " uuid)})})
      {:status 400
       :body (str {:status nil
                   :status-desc "ERROR. You need to add a \"tpid\" parameter to your request."})})))

(defn client-init-connect-handler [input]
  (let [nickname (get (:params input) "nickname")]
    (if nickname
      (let [tpid (:unique_id (first (db/tpid-from-nick {:nickname nickname})))
            uuid (first (db/tp-get-uuid {:tpid tpid}))]
        (if (:on_status (first (db/tp-get-on-status {:tpid tpid}))) ;checks if tp is available
          (if (:available_status (first (db/tp-get-availability {:tpid tpid}))) ;check if tp is on
            (do
              (db/tp-set-unavailable! {:tpid tpid})
              {:status 200
               :body (str {:status "success"
                           :status-desc (str "Successfully established connection to teleporter: " tpid)
                           :mqtt-username "nameynamename"
                           :mqtt-password "super-secret-password!"
                           :tpid tpid
                           :uuid uuid})})
            {:status 400
             :body (str {:status nil
                         :status-desc "ERROR. The teleporter you are trying to access is in use"})})
          {:status 400
           :body (str {:status nil
                       :status-desc "ERROR. The teleporter you are trying to access is turned off"})}))

      {:status 400
       :body (str {:status nil
                   :status-desc "ERROR. You need to add a \"nickname\" parameter to your request."})})))

(comment "Useful db queries for devs:"
         (db/tp-set-all-unavailable!)
         (db/tp-set-all-off!)
         (db/tp-set-all-available!)
         (db/tp-set-all-on!))
