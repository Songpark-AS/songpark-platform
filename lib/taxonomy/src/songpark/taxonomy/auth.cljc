(ns songpark.taxonomy.auth
  (:require [clojure.spec.alpha :as spec]
            [songpark.taxonomy.locale]))


(spec/def :auth.user/id int?)
(spec/def :auth.user/password string?)
(spec/def :auth.user/email string?)
(spec/def :auth.user/first-name string?)
(spec/def :auth.user/last-name string?)
(spec/def :auth.user/active? boolean?)
(spec/def :auth.user/source #{:system :oauth})
(spec/def :auth.user/license-agreed? boolean?)

(spec/def :auth/base-user (spec/keys :req [:auth.user/first-name
                                           :auth.user/last-name
                                           :auth.user/active?]
                                     ;; we do not save emails from Feide, only for
                                     ;; backoffice users
                                     :opt [:auth.user/email]))

(spec/def :auth/user (spec/merge
                      :auth/base-user
                      (spec/keys :opt [:auth.user/id
                                       :auth.user/source
                                       :auth.user/license-agreed?
                                       :locale/id])))

(spec/def :auth/auth (spec/keys :req [:auth.user/email
                                      :auth.user/password]))


;; do not show this outwards, this is used for internal consumption on the backend
(spec/def :auth/role #{:auth.role/admin :auth.role/teacher :auth.role/student})

