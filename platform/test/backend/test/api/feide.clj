(ns platform.test.api.feide
  (:require [midje.sweet :refer :all]
            [platform.api.feide :as api.feide]
            [platform.init :as init])
  (:import [java.nio.file Files]
           (ez_database.core IEzDatabase)))

(fact
  "Given a provided full-name argument, name-attributes fn"
  (fact "should splice the full name at the first space to deduce first-name and last-name"
        (api.feide/name-attributes-from "Alan Kay") => {:attrs/first-name "Alan"
                                                        :attrs/last-name  "Kay"})
  (fact "should attribute the same value for the first-name and last-name when this latter is not available"
        (api.feide/name-attributes-from "Alan") => {:attrs/first-name "Alan"
                                                    :attrs/last-name  "Alan"})
  (fact "should throw an AssertionError if the provided name is nil or empty"
        (api.feide/name-attributes-from "") => (throws AssertionError)
        (api.feide/name-attributes-from nil) => (throws AssertionError)))

(fact "Given the given-names surnames common-names displayname attributes retrieved via Feide authentication api, add-user-name-from fn"
      (fact "should return the user map with the associated :attrs/first-name and :attrs/last-name by taking respectively the first value from given-names and surnames if they are not empty and not nil"
            (let [given-names ["Alan" "Curtis"]
                  surnames ["Kay"]
                  common-names ["Alan Kay" "Alan Curtis Kay" "Alan C. Kay"]
                  display-name "Alan K"]
              (api.feide/add-user-name-from {} given-names surnames common-names display-name) => {:attrs/first-name "Alan"
                                                                                                   :attrs/last-name "Kay"}))
      (fact "should return the user map with the associated :attrs/first-name and :attrs/last-name by taking values deduced from common-names if any of given-names and surnames is nil or empty"
            (let [given-names ["Alan" "Curtis"]
                  surnames nil
                  common-names ["Alan Curtis Kay" "Alan Kay" "Alan C. Kay"]
                  display-name "Alan K"]
              (api.feide/add-user-name-from {} given-names surnames common-names display-name) => {:attrs/first-name "Alan"
                                                                                                   :attrs/last-name "Curtis Kay"}))
      (fact "should return the user map with the associated :attrs/first-name and :attrs/last-name by taking values deduced from displayname if common-names and any of given-names and surnames is nil or empty"
            (let [given-names ["Alan" "Curtis"]
                  surnames nil
                  common-names nil
                  display-name "Alan K"]
              (api.feide/add-user-name-from {} given-names surnames common-names display-name) => {:attrs/first-name "Alan"
                                                                                                   :attrs/last-name "K"})))
(fact "Given a user map"
      (fact "the following user affiliation from Feide should qualify a user as a teacher"
            (let [identity-affiliation-from-feide #{:faculty :employee :member :other}]
              (api.feide/add-user-affiliation {} identity-affiliation-from-feide) => {:attrs/teacher? true
                                                                                      :attrs/student? false}))
      (fact "the following user affiliation from Feide should qualify a user as a student"
            (let [identity-affiliation-from-feide #{:student :member :other}]
              (api.feide/add-user-affiliation {} identity-affiliation-from-feide) => {:attrs/teacher? false
                                                                                      :attrs/student? true}))
      (fact "the following user affiliation from Feide should qualify a user as both a teacher and a student"
            (let [identity-affiliation-from-feide #{:faculty :employee :student :member :other}]
              (api.feide/add-user-affiliation {} identity-affiliation-from-feide) => {:attrs/teacher? true
                                                                                      :attrs/student? true})))

(future-fact "Given the map of saml-assertions retrieved from Feide, the user-attributes-from"
             (fact "should take the assertions as input and return the matching user attributes map"
                   (let [saml-assertions {:inResponseTo "_c9c029ec886798536d71de9588668f46e7d15b1869",
                                          :status       "urn:oasis:names:tc:SAML:2.0:status:Success",
                                          :success?     true,
                                          :version      "2.0",
                                          :issueInstant #inst "2009-10-26T13:04:16.000-00:00",
                                          :destination  "https://ow.feide.no/ssp/saml2/sp/acs.php",
                                          :assertions   ({:attrs        {"cn"                   ("Edsger W Dijkstra"
                                                                                                  "edsgerW"
                                                                                                  "Edsger Wybe Dijkstra"),
                                                                         "norEduPersonNIN"      ("28089533134"),
                                                                         "feideSchoolList"      ("NO974597403" "NO974597322"),
                                                                         "eduPersonAffiliation" ("faculty"
                                                                                                  "employee"
                                                                                                  "member")},
                                                          :audiences    ("urn:mace:feide.no:services:no.feide.openwikicore"),
                                                          :name-id      {:value  "_508ddf0c3974b7a5951f5879e0796f97be449fcfdd",
                                                                         :format "urn:oasis:names:tc:SAML:2.0:nameid-format:transient"},
                                                          :confirmation {:in-response-to  "_c9c029ec886798536d71de9588668f46e7d15b1869",
                                                                         :not-before      nil,
                                                                         :not-on-or-after #inst "2009-10-26T13:09:16.000-00:00",
                                                                         :recipient       "https://ow.feide.no/ssp/saml2/sp/acs.php"}})}]
                     (api.feide/user-attributes-from saml-assertions)) => {:attrs/first-name "Edsger",
                                                                           :attrs/last-name  "W Dijkstra",
                                                                           :attrs/teacher?   true,
                                                                           :attrs/student?   false,
                                                                           :attrs/school     "NO974597403",
                                                                           :attrs/provider   :saml/feide}))


(fact "Given the school and provider attributes extracted from Feide assertions response"
      (fact "fetch-school-from fn should save the school in db if does not exist yet"
            (with-redefs [platform.model.school/by-name (fn [_ _]
                                                         nil)
                          platform.model.school/add-school (fn [_ _]
                                                            {:school/id       1
                                                             :school/name     "Eindhoven University of Technology"})]
              (let [db nil
                    attributes {:attrs/school   "Eindhoven University of Technology"
                                :attrs/provider :feide/teacher42}]
                (api.feide/fetch-school-from db attributes)) => {:school/id       1
                                                                 :school/name     "Eindhoven University of Technology"}))
      (fact "fetch-school-from fn should retrieve the school from db if it already exists"
            (with-redefs [platform.model.school/by-name (fn [_ _]
                                                         {:school/id       1
                                                          :school/name     "Eindhoven University of Technology"})]
              (let [db nil
                    attributes {:attrs/school   "Eindhoven University of Technology"
                                :attrs/provider :feide/teacher42}]
                (api.feide/fetch-school-from db attributes)) => {:school/id       1
                                                                 :school/name     "Eindhoven University of Technology"}))
      (fact "fetch-user-from fn should save the user in db if does not exist yet"
            ;; as a student
            (with-redefs [platform.model.student/by-name-and-provider (fn [_ _ _ _]
                                                                       nil)
                          platform.model.student/add-student (fn [_ _]
                                                              {:student/id         1
                                                               :student/first-name "Larry"
                                                               :student/last-name  "Wall"
                                                               :school/id          2})]
              (fact "as a student if he/she does not exist yet"
                    (let [db nil
                          attributes {:attrs/student?   true
                                      :attrs/teacher?   false
                                      :attrs/first-name "Larry"
                                      :attrs/last-name  "Wall"
                                      :attrs/school     "University of California, Berkeley"
                                      :attrs/provider   :feide/p3rl}
                          school {:school/id   2
                                  :school/name "University of California, Berkeley"}]
                      (api.feide/fetch-user-from db attributes school)) => {:student/id         1
                                                                            :student/first-name "Larry"
                                                                            :student/last-name  "Wall"
                                                                            :school/id          2}))

            ;; as a teacher
            (with-redefs [platform.model.teacher/by-name-and-provider (fn [_ _ _ _]
                                                                       nil)
                          platform.model.teacher/add-teacher (fn [_ _]
                                                              {:teacher/id         1
                                                               :teacher/first-name "Edsger"
                                                               :teacher/last-name  "Dijkstra"
                                                               :school/id          1})]
              (fact "as a teacher if he/she does not exist yet"
                    (let [db nil
                          attributes {:attrs/student?   false
                                      :attrs/teacher?   true
                                      :attrs/first-name "Edsger"
                                      :attrs/last-name  "Dijkstra"
                                      :attrs/school     "Eindhoven University of Technology"
                                      :attrs/provider   :feide/teacher42}
                          school {:school/id   1
                                  :school/name "Eindhoven University of Technology"}]
                      (api.feide/fetch-user-from db attributes school)) => {:teacher/id         1
                                                                            :teacher/first-name "Edsger"
                                                                            :teacher/last-name  "Dijkstra"
                                                                            :school/id          1})))

      (fact "fetch-user-from fn should retrieve the user from db if he/she has previously been registered"
            ;; as a student
            (with-redefs [platform.model.student/by-name-and-provider (fn [_ _ _ _]
                                                                       {:student/id         1
                                                                        :student/first-name "Larry"
                                                                        :student/last-name  "Wall"
                                                                        :school/id          2})]
              (fact "as a student"
                    (let [db nil
                          attributes {:attrs/student?   true
                                      :attrs/teacher?   false
                                      :attrs/first-name "Larry"
                                      :attrs/last-name  "Wall"
                                      :attrs/school     "University of California, Berkeley"
                                      :attrs/provider   :feide/p3rl}
                          school {:school/id   2
                                  :school/name "University of California, Berkeley"}]
                      (api.feide/fetch-user-from db attributes school)) => {:student/id         1
                                                                            :student/first-name "Larry"
                                                                            :student/last-name  "Wall"
                                                                            :school/id          2}))

            ;; as a teacher
            (with-redefs [platform.model.teacher/by-name-and-provider (fn [_ _ _ _]
                                                                       {:teacher/id         1
                                                                        :teacher/first-name "Edsger"
                                                                        :teacher/last-name  "Dijkstra"
                                                                        :school/id          1})]
              (fact "as a teacher"
                    (let [db nil
                          attributes {:attrs/student?   false
                                      :attrs/teacher?   true
                                      :attrs/first-name "Edsger"
                                      :attrs/last-name  "Dijkstra"
                                      :attrs/school     "Eindhoven University of Technology"
                                      :attrs/provider   :feide/teacher42}
                          school {:school/id   1
                                  :school/name "Eindhoven University of Technology"}]
                      (api.feide/fetch-user-from db attributes school)) => {:teacher/id         1
                                                                            :teacher/first-name "Edsger"
                                                                            :teacher/last-name  "Dijkstra"
                                                                            :school/id          1}))))
(fact "Given the assertions attributes retrieved from Feide"
      (with-redefs [platform.model.school/by-name (fn [_ _]
                                                   nil)
                    platform.model.school/add-school (fn [_ _]
                                                      {:school/id       1
                                                       :school/name     "Eindhoven University of Technology"})
                    platform.model.teacher/by-name-and-provider (fn [_ _ _ _]
                                                                 nil)
                    platform.model.teacher/add-teacher (fn [_ _]
                                                        {:teacher/id         1
                                                         :teacher/first-name "Edsger"
                                                         :teacher/last-name  "Dijkstra"
                                                         :school/id          1})]
        (fact "a teacher identity is retrieved with the following attributes"
              (let [db nil
                    feide-assertion-attributes {:attrs/student?   false
                                                :attrs/teacher?   true
                                                :attrs/first-name "Edsger"
                                                :attrs/last-name  "Dijkstra"
                                                :attrs/provider   :feide/teacher42
                                                :attrs/school     "Eindhoven University of Technology"}]
                (api.feide/attrs->identity db feide-assertion-attributes)) => {:teacher/id        1
                                                                               :authz/credentials #{:teacher}
                                                                               :school/id         1}))
      (with-redefs [platform.model.school/by-name (fn [_ _]
                                                   nil)
                    platform.model.school/add-school (fn [_ _]
                                                      {:school/id       2
                                                       :school/name     "University of California, Berkeley"})
                    platform.model.student/by-name-and-provider (fn [_ _ _ _]
                                                                 nil)
                    platform.model.student/add-student (fn [_ _]
                                                        {:student/id         1
                                                         :student/first-name "Larry"
                                                         :student/last-name  "Wall"
                                                         :school/id          2})]
        (fact "a student identity is retrieved with the following attributes"
              (let [db nil
                    feide-assertion-attributes {:attrs/student?   true
                                                :attrs/teacher?   false
                                                :attrs/first-name "Larry"
                                                :attrs/last-name  "Wall"
                                                :attrs/provider   :feide/p3rl
                                                :attrs/school     "University of California, Berkeley"}]
                (api.feide/attrs->identity db feide-assertion-attributes)) => {:student/id        1
                                                                               :authz/credentials #{:student}
                                                                               :school/id         2})))