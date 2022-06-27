(ns platform.model.teleporter
  (:require [ez-database.core :as db]
            [ez-database.transform :as transform]
            [taoensso.timbre :as log]))


(transform/add :setting :teleporter/setting
               [:teleporter_id :teleporter/id]
               [:name          :teleporter/nickname])

(defn get-settings [db user-id]
  (->> {:select [:*]
        :from [:teleporter_settings]
        :where [:= :user_id user-id]}
       (db/query db ^:opts {[:transformation :post]
                            [:setting :teleporter/setting]})
       first))

(defn save-settings [db user-id data]
  (let [result (db/query! db {:update :teleporter_settings
                              :set {:name (:teleporter/nickname data)}
                              :where [:and
                                      [:= :teleporter_id (:teleporter/id data)]
                                      [:= :user_id user-id]]})]
    (if-not (empty? result)
      (get-settings db user-id)
      false)))


(comment

  (let [db (:database @platform.init/system)]
    (get-settings db 1))

  )
