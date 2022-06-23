(ns platform.model.auth
  (:require [buddy.hashers]
            [ez-database.core :as db]
            [ez-database.transform :as transform]
            [platform.auth :as auth]
            [platform.config :refer [config]]
            [platform.email.mandrill :as mandrill]
            [platform.util :refer [id->uuid
                                   get-url]]
            [taoensso.timbre :as log]
            [tick.core :as t]))

(transform/add :user :auth/user
               [:id               :auth.user/id]
               [:email            :auth.user/email]
               [:verified_email_p :auth.user/verified-email?]
               [:name             :profile/name]
               [:position         :profile/position])


(defn- get-email-hours []
  (get-in config [:auth :email-token-hours] (* 365 24 10)))

(defn- get-password-hours []
  (get-in config [:auth :password-token-hours] 2))

(defn get-user [db user-id]
  (let [user (->> {:select [:u.id :u.email :u.verified_email_p
                            :p.name :p.position :p.pronoun_id]
                   :from [[:auth_user :u]]
                   :left-join [[:profile_profile :p] [:= :p.user_id :u.id]]
                   :where [:= :u.id user-id]}
                  (db/query db ^:opts {[:transformation :post]
                                       [:user :auth/user]})
                  first)]
    (assoc user :auth.user/channel (id->uuid (:auth.user/id user)))))

(defn user-exists? [db email]
  (->> {:select [:id]
        :from [:auth_user]
        :where [:= :email email]}
       (db/query db)
       first
       some?))

(defn send-verified-email! [db user-id]
  (let [token (auth/get-token)
        {username :name
         email :email} (->> {:select [:p.name :u.email]
                             :from [[:auth_user :u]]
                             :join [[:profile_profile :p] [:= :p.user_id :u.id]]
                             :where [:= :u.id user-id]}
                            (db/query db)
                            first)]
    (do
      (->> {:update :auth_user
            :set {:verified_email_token token
                  :verified_email_token_at (t/now)}
            :where [:= :id user-id]}
           (db/query! db))
      ;; send out email for verification of email
      (log/debug {:email email
                  :username username
                  :token token})
      (mandrill/post "/messages/send-template"
                     {:template_name "verification-of-email"
                      :template_content []
                      :message {:to [{:email email :type "to"}]
                                :merge true
                                :merge_vars [{:rcpt email
                                              :vars [{:name "NAME"
                                                      :content username}
                                                     {:name "TOKEN"
                                                      :content token}]}]}}))))

(defn signup [db {:auth.user/keys [email password]
                  :profile/keys [name]}]
  (try
    (db/with-transaction [db :default]
      (let [user (->> {:insert-into :auth_user
                       :values [{:email email
                                 :password (buddy.hashers/derive password)}]}
                      (db/query<! db)
                      first)]
        (db/query! db {:insert-into :profile_profile
                       :values [{:user_id (:id user)
                                 :name name
                                 :position ""
                                 :image_url ""}]})
        (send-verified-email! db (:id user))
        (get-user db (:id user))))
    (catch Exception e
      (log/warn "Failed to create a user" {:email email
                                           :exception e
                                           :message (ex-message e)
                                           :data (ex-data e)})
      false)))

(comment

  (let [db (:database @platform.init/system)
        data {:auth.user/email "emil0r@gmail.com"
              :auth.user/password "foobar"
              :profile/name "Emil Bengtsson"}]
    (signup db data))
  )


(defn login [db {:auth.user/keys [email password]}]
  (let [user (->> {:select [:u.id :u.email :u.verified_email_p :u.password
                            :p.name :p.position :p.pronoun_id]
                   :from [[:auth_user :u]]
                   :left-join [[:profile_profile :p] [:= :p.user_id :u.id]]
                   :where [:= :u.email email]}
                  (db/query db)
                  first)]
    (if (buddy.hashers/check password (:password user))
      (assoc (transform/transform :user :auth/user user)
             :auth.user/channel (id->uuid (:id user)))
      false)))


(defn verify-email [db {:auth.user/keys [token]}]
  (let [token-at (t/<< (t/now)
                       (t/new-duration (get-email-hours) :hours))]
    (if-not (->> {:update :auth_user
                  :set {:verified_email_p true
                        :verified_email_token_at nil
                        :verified_email_token nil}
                  :where [:and
                          [:= :verified_email_token token]
                          [:>= :verified_email_token_at token-at]]}
                 (db/query! db)
                 first
                 zero?)
      true
      false)))

(defn send-verify-email [db {:auth.user/keys [email]}]
  (let [user-id (->> {:select [:id]
                      :from [:auth_user]
                      :where [:= :email email]}
                     (db/query db)
                     first
                     :id)]
    (send-verified-email! db user-id)
    true))

(defn change-password [db user-id {:auth.user/keys [password new-password]}]
  (let [user (->> {:select [:password]
                   :from [:auth_user]
                   :where [:= :id user-id]}
                  (db/query db)
                  first)
        same-password? (if user
                         (buddy.hashers/check password (:password user))
                         false)]
    (if (and user same-password?)
      (do (db/query! db {:update :auth_user
                         :set {:password (buddy.hashers/derive new-password)}
                         :where [:= :id user-id]})
          true)
      false)))

(defn forgotten-password [db {:auth.user/keys [email]}]
  (let [token (auth/get-token)
        username (->> {:select [:p.name]
                       :from [[:profile_profile :p]]
                       :join [[:auth_user :u] [:= :p.user_id :u.id]]
                       :where [:= :u.email email]}
                      (db/query db)
                      first
                      :name)]
    (db/query! db {:update :auth_user
                   :set {:token token
                         :token_at (t/now)}
                   :where [:= :email email]})
    (mandrill/post "/messages/send-template"
                   {:template_name "forgotten-password"
                    :template_content []
                    :message {:to [{:email email :type "to"}]
                              :merge true
                              :merge_vars [{:rcpt email
                                            :vars [{:name "NAME"
                                                    :content username}
                                                   {:name "TOKEN"
                                                    :content token}]}]}})))

(defn reset-password [db {:auth.user/keys [new-password token]}]
  (let [token-at (t/<< (t/now)
                       (t/new-duration (get-password-hours) :hours))
        user (->> {:select [:id]
                   :from [:auth_user]
                   :where [:and
                           [:= :token token]
                           [:>= :token_at token-at]]}
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
    (get-user db 1)
    #_(login db {:auth.user/email "emil0r@gmail.com"
                 :auth.user/password "asdf"})
    #_(verify-email db {:auth.user/token "165827"})
    #_(change-password db 1 {:auth.user/password "foobar"
                           :auth.user/new-password "foobar"})
    #_(forgotten-password db {:auth.user/email "emil0r@gmail.com"})
    #_(reset-password db {:auth.user/token "409348"
                        :auth.user/new-password "meh"})
    )
  )
