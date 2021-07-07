(ns platform.model.auth
  (:require [buddy.hashers :as hashers]
            [clojure.spec.alpha :as spec]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [songpark.taxonomy.auth]))


(transform/add :auth-user :auth.user/user
               [:id            :auth.user/id]
               [:active_p      :auth.user/active?]
               [:email         :auth.user/email]
               [:first_name    :auth.user/first-name]
               [:last_name     :auth.user/last-name])

(defn user-by-id [db id]
  (db/query db
            (with-meta (array-map
                        ;; transform the data we get out into :auth.user/user
                        [:transformation :post]
                        [:auth-user :auth.user/user]
                        ;; we only want one result back
                        [:post :one] nil)
              {:opts true})
            {:select [:*]
             :from [:auth_user]
             :where [:= :id id]}))

(defn user-by-email [db email]
  (db/query db 
            (with-meta (array-map
                        ;; transform the data we get out into :auth.user/user
                        [:transformation :post]
                        [:auth-user :auth.user/user]
                        ;; we only want one result back
                        [:post :one] nil)
              {:opts true})
            {:select [:*]
             :from [:auth_user]
             :where [:= :email email]}))


(defn users [db]
  (db/query db
            ^:opts {[:transformation :post]
                    [:auth-user :auth.user/user]}
            {:select [:*]
             :from [:auth_user]
             :order-by [[:id :desc]]}))


(defn add-user! [db user]
  (let [{:auth.user/keys [password active? first-name last-name email]} user]
    (prn :adding-user!!!!!!!!! user db)
    (db/query! db {:insert-into :auth_user
                   :values [{:password (hashers/encrypt password)
                             :first_name first-name
                             :active_p active?
                             :last_name last-name
                             :email email}]})))

(defn check-password [db email password]
  (let [hashed-pw (->> {:select [:password]
                        :from [:auth_user]
                        :where [:= :email email]}
                       (db/query db)
                       first
                       :password)]
    (hashers/check password hashed-pw)))
