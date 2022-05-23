(ns platform.model.auth
  (:require [buddy.hashers]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [tick.core :as t]))

(transform/add :user :auth/user
               [:id               :auth.user/id]
               [:email            :auth.user/email]
               [:verified_email_p :auth.user/verified-email?]
               [:name             :profile/name]
               [:bio              :profile/bio]
               [:location         :profile/location])

(defn login [db {:auth.user/keys [email password]}]
  (let [user (->> {:select [:u.id :u.email :u.verified_email_p :u.password
                            :p.name :p.location :p.bio :p.pronoun_id]
                   :from [[:auth_user :u]]
                   :left-join [[:profile_profile :p] [:= :p.user_id :u.id]]
                   :where [:= :u.email email]}
                  (db/query db)
                  first)]
    (if (buddy.hashers/check password (:password user))
      (transform/transform :user :auth/user user)
      false)))

(defn verify-email [db {:auth.user/keys [token]}]
  (assert (uuid? token) "token must be a UUID")
  (if-not (->> {:update :auth_user
                :set {:verified_email_p true
                      :verified_email_token nil}
                :where [:= :verified_email_token token]}
               (db/query! db)
               first
               zero?)
    true
    false))

(defn change-password [db {:auth.user/keys [id password new-password]}]
  (let [user (->> {:select [:id :password :email]
                      :from [:auth_user]
                      :where [:= :id id]}
                     (db/query db)
                     first)
        same-password? (if user
                         (buddy.hashers/check password (:password user))
                         false)]
    (if (and user same-password?)
      (do (db/query! db {:update :auth_user
                         :set {:password (buddy.hashers/derive new-password)}
                         :where [:= :id id]})
          true)
      false)))

(defn forgotten-password [db {:auth.user/keys [email]}]
  (db/query! db {:update :auth_user
                 :set {:token (java.util.UUID/randomUUID)
                       :token_at (t/now)}
                 :where [:= :email email]}))

(defn reset-password [db {:auth.user/keys [new-password token]}]
  (let [user (->> {:select [:id]
                   :from [:auth_user]
                   :where [:and
                           [:= :token token]
                           [:>= :token_at (t/<< (t/now)
                                                (t/new-duration 2 :hours))]]}
                  (db/query db)
                  first)]
    (if user
      ;; reset password and set token and token_at to nil if there is a user
      (do (db/query! db {:update :auth_user
                         :set {:token nil
                               :token_at nil
                               :password (buddy.hashers/derive new-password)}
                         :where [:= :token token]})
          true)
      ;; no user found. reset the token and token_at to nil
      ;; this will not always hit, but will hit if there is an expired token
      ;; in addition, there should be a job cleaning this up
      (do (db/query! db {:update :auth_user
                         :set {:token nil
                               :token_at nil}
                         :where [:= :token token]})
          false))))

(comment

  (let [db (:database @platform.init/system)]
    #_(login db {:auth.user/email "emil0r@gmail.com"
                 :auth.user/password "foobar"})
    (verify-email db {:auth.user/token #uuid "8149af03-09ea-4548-a78f-b39ff9cb0952"})
    #_(change-password db {:auth.user/id 1
                         :auth.user/password "exam"
                           :auth.user/new-password "foobar"})
    #_(forgotten-password db {:auth.user/email "emil0r@gmail.com"})
    #_(reset-password db {:auth.user/token #uuid "cde67abf-ed21-4997-9fcc-b37b82ce1520"
                        :auth.user/new-password "meh"})
    )
  )

(defn update-password [db {:auth.user/keys [id password]}])
