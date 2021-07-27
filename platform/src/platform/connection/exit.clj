(ns platform.connection.exit
  (:require [platform.model.auth :as model.auth]
            [taoensso.timbre :as log]
            [songpark.taxonomy.auth]
            [yesql.core :refer [defqueries]]
            [platform.connection.parsesql :as db]))

(defn tp-disconnect-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (case tpid 
      nil {:status 400
           :body (str {:status "ERROR-missing-tpid-parameter"})}
      "all" (do
              (db/tp-set-all-available!)
              {:status 200
               :body (str {:status "success"})})
      (do ;;default
        (db/tp-set-available! {:tpid tpid})
        {:status 200
         :body (str {:status "success"})})
      )))

(defn tp-turnoff-handler [input]
  (let [tpid (get (:params input) "tpid")]
    (case tpid
      nil {:status 400
           :body (str {:status "ERROR-missing-tpid-parameter"})}
      "all" (do
              (db/tp-set-all-unavailable!)
              (db/tp-set-all-off!)
              {:status 200
               :body (str {:status "success"})})
      (do ;;default
        (db/tp-set-unavailable! {:tpid tpid})
        (db/tp-set-off! {:tpid tpid})
        {:status 200
         :body (str {:status "success"})}))))