(ns platform.test.model.oauth
  (:require [midje.sweet :refer :all]
            [platform.model.oauth :as model.oauth]
            [platform.test.util :as util]))


(def student-data
  {:id "55de7d71-4a25-4103-8e43-35df8c2d472a"
   :first-name "Asbjørn ElevG"
   :last-name "Hansen"
   :email "test@mail.com"
   :groups [{:id "856326499"
             :affiliation :student
             :name "Grøn barneskole"}]})


(def teacher-data
  {:id "55de7d71-4a25-4103-8e43-35df8c2d4724"
   :first-name "Teacher"
   :last-name "McTeacher"
   :email "test@mail.com"
   :groups [{:id "856326499"
             :affiliation :teacher
             :name "Grøn barneskole"}]})



(fact "oauth db"
      (let [db (util/start-db)]
        (try
          (util/prepare-db! db)
          (fact "is student?"
                (model.oauth/student? student-data) => true)
          (fact "is teacher?"
                (model.oauth/teacher? teacher-data) => true)
          (fact "teacher exists?"
                (model.oauth/teacher-exists? db student-data) => false)
          (fact "student exists?"
                (model.oauth/student-exists? db student-data) => false)
          (fact "schools exists?"
                (model.oauth/schools-exists? db student-data) => false)
          (fact "add-schools"
                (model.oauth/add-schools db student-data)
                (model.oauth/schools-exists? db student-data) => true)
          (fact "add-student"
                (model.oauth/add-student db student-data)
                (model.oauth/student-exists? db student-data) => true)
          (fact "add-teacher"
                (model.oauth/add-teacher db teacher-data)
                (model.oauth/teacher-exists? db teacher-data) => true)
          (finally
            (util/stop-db db)))))

(def feide-data {:id "f62f2169-d246-432d-90eb-743284a7bd83",
                 :first-name "Bjørg LærerG",
                 :last-name "Olsen",
                 :email "bjorg_laererg@test.feide.no",
                 :groups [{:id "956326500",
                           :affiliation :unknown,
                           :name "Hassel barneskole"}]})

;; (fact "oauth handle-feide-data"
;;       (let [db (util/start-db)]
;;         (try
;;           (util/prepare-db! db)
;;           (finally
;;             (util/stop-db db)))))
