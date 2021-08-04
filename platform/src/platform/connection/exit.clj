(ns platform.connection.exit
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as db]))

(defn tp-disconnect-handler [input]
  (let [parameters (:params input)
        tpid (get parameters "tpid")
        uuid (.toString (java.util.UUID/randomUUID))]
    (case tpid
      nil {:status 400
           :body (str {:status nil
                       :status-desc "ERROR. You need to add a \"tpid\" parameter to your request."})}
      "all" (do
              (db/tp-set-all-available!)
              {:status 200
               :body (str {:status "success"
                           :status-desc "Successfully updated the database. Every teleporter is now available"})})
      (do ;;default
        (db/tp-set-available! {:tpid tpid :uuid uuid})
        {:status 200
         :body (str {:status "success"
                     :status-desc (str "Successfully updated the database. Teleporter:" tpid " is now available with uuid:" uuid)
                     :uuid uuid})})
      )))

(defn tp-turnoff-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (case tpid
      nil {:status 400
           :body (str {:status nil
                       :status-desc "ERROR. You need to add a \"tpid\" parameter to your request."})}
      "all" (do
              (db/tp-set-all-unavailable!)
              (db/tp-set-all-off!)
              {:status 200
               :body (str {:status "success"
                           :status-desc "Successfully updated the database. Every teleporter is now turned off and unavailable"})})
      (do ;;default
        (db/tp-set-unavailable! {:tpid tpid})
        (db/tp-set-off! {:tpid tpid})
        {:status 200
         :body (str {:status "success"
                     :status-desc (str "Successfully updated the database. Teleporter:" tpid " is now turned off and unavailable")})}))))