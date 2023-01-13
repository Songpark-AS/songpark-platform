(ns platform.model.pairing
  (:require [ez-database.core :as db]
            [ez-database.transform :as transform]
            [platform.util :refer [serial->uuid]]
            [taoensso.timbre :as log]))

(transform/add :pairing :pairing/paired
               [:teleporter_id  :teleporter/id]
               [:name           :teleporter/nickname])

(defn already-paired? [db user-id serial]
  (let [teleporter-id (serial->uuid serial)]
    (->> {:select [:user_id]
          :from [:teleporter_pairing]
          :where [:and
                  [:= :user_id user-id]
                  [:= :teleporter_id teleporter-id]]}
         (db/query db)
         first
         some?)))

(defn serial-exists? [db serial]
  (->> {:select [:serial]
        :from [:teleporter_teleporter]
        :where [:= :serial serial]}
       (db/query db)
       first
       some?))

(defn- settings-exists? [db user-id teleporter-id]
  (->> {:select [:teleporter_id]
        :from [:teleporter_settings]
        :where [:and
                [:= :user_id user-id]
                [:= :teleporter_id teleporter-id]]}
       (db/query db)
       first
       some?))

(defn get-pairs [db user-id]
  (->> {:select [:p.*, :s.name]
        :from [[:teleporter_pairing :p]]
        :join [[:teleporter_settings :s] [:and
                                          [:= :p.user_id :s.user_id]
                                          [:= :p.teleporter_id :s.teleporter_id]]]
        :where [:= :p.user_id user-id]}
       (db/query db ^:opts {[:transformation :post]
                            [:pairing :pairing/paired]})))

(defn get-pair [db user-id teleporter-id-or-serial]
  (let [teleporter-id (if (uuid? teleporter-id-or-serial)
                        teleporter-id-or-serial
                        (serial->uuid teleporter-id-or-serial))]
    (->> {:select [:p.*, :s.name]
          :from [[:teleporter_pairing :p]]
          :join [[:teleporter_settings :s] [:and
                                            [:= :p.teleporter_id :s.teleporter_id]
                                            [:= :p.user_id :s.user_id]]]
          :where [:and
                  [:= :p.user_id user-id]
                  [:= :p.teleporter_id teleporter-id]]}
         (db/query db ^:opts {[:transformation :post]
                              [:pairing :pairing/paired]})
         first)))

(defn pair [db user-id teleporter-id]
  (let [settings? (settings-exists? db user-id teleporter-id)]
    (db/with-transaction [db :default]
      ;; only allow one pairing at a time
      (db/query! db {:delete-from :teleporter_pairing
                     :where [:= :teleporter_id teleporter-id]})
      (db/query! db {:insert-into :teleporter_pairing
                     :values [{:user_id user-id
                               :teleporter_id teleporter-id}]})
      (when-not settings?
        (let [serial (->> {:select [:serial]
                           :from [:teleporter_teleporter]
                           :where [:= :id teleporter-id]}
                          (db/query db)
                          first
                          :serial)]
          (db/query! db {:insert-into :teleporter_settings
                         :values [{:teleporter_id teleporter-id
                                   :user_id user-id
                                   :name (str "TP" serial)}]}))))
    (get-pair db user-id teleporter-id)))

(defn unpair [db user-id]
  (db/query! db {:delete-from :teleporter_pairing
                 :where [:= :user_id user-id]}))

(defn cut-pairing
  "Cut all pairing to a teleporter to all users"
  [db teleporter-id]
  (let [user-ids (->> {:select [:user_id]
                       :from [:teleporter_pairing]
                       :where [:= :teleporter_id teleporter-id]}
                      (db/query db)
                      (map :user_id))]
    (db/query! db {:delete-from :teleporter_pairing
                   :where [:= :teleporter_id teleporter-id]})
    user-ids))


(comment
  (let [db (:database @platform.init/system)]
    (get-pairs db 3))

  (let [db (:database @platform.init/system)]
    (pair db 1 (serial->uuid "0001")))

  (let [db (:database @platform.init/system)]
    (get-pairs db 1))

  (let [db (:database @platform.init/system)]
    (unpair db 1))

  (let [db (:database @platform.init/system)]
    (already-paired? db 1 "0001"))

  (let [db (:database @platform.init/system)]
    (serial-exists? db "0001"))


  )
