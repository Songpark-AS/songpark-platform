(ns platform.test.model.auth
  (:require [platform.model.auth :as auth]
            [platform.test.util :as util]
            [ez-database.core :as db]
            [midje.sweet :refer :all]))

(fact "database.auth"
      (let [db (util/start-db)
            user #:auth.user{:email "test@test.com" :first-name "Test" :last-name "McTest" :active? true :password "test"}
            expected-user (-> user
                              (dissoc :auth.user/password)
                              (assoc :auth.user/id 1))]
        (try
          (util/prepare-db! db)

          (fact "add-user!"
                (auth/add-user! db user) => [1])
          (fact "get-user-by-email"
                (auth/user-by-email db "test@test.com") => expected-user)
          (fact "get-user-by-id"
                (auth/user-by-id db 1) => expected-user)
          (fact "check-password?"
                (auth/check-password db "test@test.com" "test"))
          
          (finally
            (util/stop-db db)))))
