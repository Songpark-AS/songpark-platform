(ns platform.api.app
  (:require [platform.model.app :as model.app]
            [songpark.taxonomy.teleporter]
            [taoensso.timbre :as log]))


;; Highly ad hoc!
(defn connect [{{db :database
                 memdb :db} :data
                {user-id :auth.user/id} :identity
                :as request}]
  (let [result (model.app/app-status db memdb user-id)]
    {:status 200
     :body result}))



(comment

  (let [db (get-in @platform.init/system [:http-server :db])]
    (connect {:data {:db db}}))
  )
