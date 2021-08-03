(ns platform.connection.initial
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as db]))


(defn tp-init-connect-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (if tpid
      (if (= tpid "all")
        (do 
          (db/tp-set-all-available!)
          (db/tp-set-all-on!)
          {:status 200
           :body (str {:status "success"
                       :status-desc "Successfully updated the database. Every teleporter is now on and available"})})
        (let [rows-updated-on (db/tp-set-on! {:tpid tpid})
            rows-updated-available (db/tp-set-available! {:tpid tpid})]        
        {:status 200
         :body (str {:status "success"
                     :status-desc (str "Successfully updated the database. Teleporter:" tpid " is now on and available.")})}))
      {:status 400
       :body (str {:status nil
                   :status-desc "ERROR. You need to add a \"tpid\" parameter to your request."})})))

(defn client-init-connect-handler [input]
  (let [nickname (get (:params input) "nickname")]
    (if nickname
      (let [tpid (:unique_id (first (db/tpid-from-nick {:nickname nickname})))]
        (if (:on_status (first (db/tp-get-on-status {:tpid tpid}))) ;checks if tp is available
          (if (:available_status (first (db/tp-get-availability {:tpid tpid}))) ;check if tp is on
           (do
            (db/tp-set-unavailable! {:tpid tpid})
            {:status 200
             :body (str {:status "success" 
                         :status-desc (str "Successfully established connection to teleporter: " tpid)
                         :mqtt-username "nameynamename" 
                         :mqtt-password "super-secret-password!" 
                         :tpid tpid})})
            {:status 400
             :body (str {:status nil
                         :status-desc "ERROR. The teleporter you are trying to access is in use"})}
            )
          {:status 400
           :body (str {:status nil
                       :status-desc "ERROR. The teleporter you are trying to access is turned off"})}))
      
      {:status 400
       :body (str {:status nil
                   :status-desc "ERROR. You need to add a \"nickname\" parameter to your request."})})))
