(ns platform.model.signup
  (:require [buddy.hashers]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [tick.core :as t]))

(defn signup [db {:auth.user/keys [email password]
                  :profile/keys [name]}]
  (db/with-transaction [db :default]
    (let [user (->> {:insert-into :auth_user
                     :values [{:email email
                               :password (buddy.hashers/derive password)
                               :verified_email_token (java.util.UUID/randomUUID)
                               :verified_email_token_at (t/now)}]}
                    (db/query<! db)
                    first)]
      (db/query! db {:insert-into :profile_profile
                     :values [{:user_id (:id user)
                               :name name
                               :bio ""
                               :image_url ""
                               :location ""}]}))
    ;; send out email for verification of email
    ))

(comment

  (let [db (:database @platform.init/system)
        data {:auth.user/email "emil0r@gmail.com"
              :auth.user/password "foobar"
              :profile/name "Emil Bengtsson"}]
    (signup db data))

  )
