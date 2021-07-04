(ns platform.test.util
  (:require [platform.aws :as aws]
            [platform.database :as database]
            [platform.google :as google]
            [platform.migrator :as migrator]
            [platform.model.auth :as model.auth]
            [buddy.core.codecs.base64 :refer [encode decode]]
            [cprop.source :refer [from-system-props from-env]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [migratus.core :as migratus]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t])
  (:import [java.nio.file Files]))

(log/set-level! :warn)

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      (last vs))))

(defn b64-encode [path]
  (encode (Files/readAllBytes (.toPath (io/file (io/resource path))))))

(defn- read-config []
  (-> (io/resource "config.edn")
      (slurp)
      (edn/read-string)))


(defn start-db []
  (let [db-config (deep-merge
                   {:datasource {:default {:maximum-pool-size 3
                                           :adapter "postgresql"
                                           :username "postgres"
                                           :password "postgres"
                                           :database-name "test"
                                           :server-name "localhost"
                                           :port-number 5432}}}
                   (select-keys (:database (from-env)) [:datasource]))
        datasource (:datasource db-config)]
    (log/merge-config! {:level :info})
    (log/info "Starting database for testing")
    (component/start (database/database {:db-specs {:default {}}
                                         :ds-specs datasource}))))

(defn stop-db [db]
  (log/info "Stopping database for testing")
  (log/merge-config! {:level :debug
                      :ns-blacklist ["org.eclipse.jetty.*"
                                     "io.grpc.netty.shaded.io.netty.*"
                                     "org.opensaml.*"]})
  (component/stop db))

(defn start-aws []
  (log/info "Starting AWS client for testing")
  (component/start (aws/aws-client (:aws (read-config)))))

(defn stop-aws [aws]
  (log/info "Stopping AWS client for testing")
  (component/stop aws))

(defn start-google []
  (log/info "Starting Google client for testing")
  (component/start (google/google-client (:google (read-config)))))

(defn stop-google [google]
  (log/info "Stopping Google client for testing")
  (component/stop google))

(defn cleanup-db!
  "Clean up database"
  [db]
  (let [ds (get-in db [:db-specs :default :datasource])
        migration-ids (try
                        ;; if the database is entirely new, there will
                        ;; be no migrations table
                        (->> {:select [:id]
                              :from [:migrations]
                              :order-by [[:id :desc]]}
                             (db/query db)
                             (mapv :id))
                        (catch Exception _
                          nil))]
    (when migration-ids
      (apply migratus/down (migrator/get-migration-map ds) migration-ids))))


(defn run-migrations! [db]
  (component/start (migrator/migration-manager {:database db})))


(defn prepare-db! [db]
  (cleanup-db! db)
  (run-migrations! db))


(defn seed-db! [db]
  (log/info "Seeding the database")
  ;; skip spam
  (log/set-level! :warn)
  (prepare-db! db)
  (let [words ["ski"
               "skifer"
               "skift"
               "skikk"
               "skildre"
               "skille"
               "skilt"
               "skimte"
               "skinn"
               "skip"]]
    (doseq [query [ ;; school
                   {:insert-into :school_school
                    :values [{:name "Hassel barneskole"
                              :provider_id "956326500"}]}
                   ;; feide test user bjorg_laererg
                   {:insert-into :school_teacher
                    :values [{:first_name "Bjørg LærerG"
                              :last_name "Olsen"
                              :provider_id "f62f2169-d246-432d-90eb-743284a7bd83"
                              :school_id 1}]}
                   ;; feide test user ann_elevg
                   ;; student id 1
                   {:insert-into :school_student
                    :values [{:first_name "Anne ElevG"
                              :last_name "Berntsen"
                              :provider_id "0132b35f-8479-4086-af68-2e4a71ff1906"
                              :school_id 1}]}
                   ;; student id 2
                   {:insert-into :school_student
                    :values [{:first_name "Student 2 testing"
                              :last_name "McStudent testing"
                              :provider_id "Test/student2"
                              :school_id 1}]}
                   ;; student id 3
                   {:insert-into :school_student
                    :values [{:first_name "Student 3 testing"
                              :last_name "McStudent testing"
                              :provider_id "Test/student3"
                              :school_id 1}]}
                   ;; feide test user cecilie_elevvgs
                   ;; student id 4
                   {:insert-into :school_student
                    :values [{:first_name "Cecilie ElevVGS"
                              :last_name "Ås"
                              :provider_id "2840803a-0f31-49fa-8d46-e3c28c067c12"
                              :school_id 1}]}
                   ;; student id 5
                   {:insert-into :school_student
                    :values [{:first_name "Student 5 testing"
                              :last_name "McStudent testing"
                              :provider_id "Test/student5"
                              :school_id 1}]}
                   ;; student id 6
                   {:insert-into :school_student
                    :values [{:first_name "Student 6 testing"
                              :last_name "McStudent testing"
                              :provider_id "Test/student6"
                              :school_id 1}]}
                   ;; feide test user alf_elevg
                   ;; student id 7
                   {:insert-into :school_student
                    :values [{:first_name "Alf ElevG"
                              :last_name "Christensen"
                              :provider_id "8a1ec21-1aa4-45b5-a168-61eec4293e30"
                              :school_id 1}]}
                   ;; feide test user jan_elevvgs
                   ;; student id 8
                   {:insert-into :school_student
                    :values [{:first_name "Jan ElevVGS"
                              :last_name "Olsen"
                              :provider_id "af34c49d-4680-4288-a3e0-b5bc8f7105bd"
                              :school_id 1}]}
                   {:insert-into :school_student
                    :values (map (fn [idx]
                                   {:first_name (str "Student " idx " testing")
                                    :last_name "McStudent testing"
                                    :provider_id (str "Test/student" idx)
                                    :school_id 1})
                                 (range 9 100))}
                   {:insert-into :school_group
                    :values [{:name "Group testing"
                              :school_id 1
                              :owner_id 1}]}
                   {:insert-into :school_group_students
                    :values [{:group_id 1
                              :student_id 1}
                             {:group_id 1
                              :student_id 2}
                             {:group_id 1
                              :student_id 3}
                             {:group_id 1
                              :student_id 4}
                             {:group_id 1
                              :student_id 5}
                             {:group_id 1
                              :student_id 6}
                             {:group_id 1
                              :student_id 7}]}

                   ;; setup locales
                   {:insert-into :locale_locale
                    :values [{:id "nb-NO"
                              :name "Norsk (bokmål)"}
                             {:id "nn-NO"
                              :name "Norsk (nynorsk)"}]}

                   ;; setup informational text
                   {:insert-into :informational_informational
                    :values [{:title "Informational title 1 text nb NO"
                              :body "Informational body 1 text nb NO"
                              :url ""
                              :type "examination.instruction/student"
                              :locale "nb-NO"}
                             {:title "Informational title 1 text nn NO"
                              :body "Informational body 1 text nn NO"
                              :url ""
                              :type "examination.instruction/student"
                              :locale "nn-NO"}
                             {:title "Informational title 2 text nb NO"
                              :body "Informational body 2 text nb NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nb-NO"}
                             {:title "Informational title 2 text nn NO"
                              :body "Informational body 2 text nn NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nn-NO"}
                             {:title "Informational title 3 text nb NO"
                              :body "Informational body 3 text nb NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nb-NO"}
                             {:title "Informational title 3 text nn NO"
                              :body "Informational body 3 text nn NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nn-NO"}
                             {:title "Informational title 4 text nb NO"
                              :body "Informational body 4 text nb NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nb-NO"}
                             {:title "Informational title 4 text nn NO"
                              :body "Informational body 4 text nn NO"
                              :url ""
                              :type "examination.instruction/parent"
                              :locale "nn-NO"}
                             {:title "Skj-lyd"
                              :body "Mål: Å kunne gjennkjenne og lese skj som en lyd.

Eksempel: Skjære, Skjelve"
                              :url ""
                              :type "examination/info"
                              :locale "nb-NO"}
                             {:title ""
                              :body "Lyden må utales uten at..."
                              :url ""
                              :type "examination/requirements"
                              :locale "nb-NO"}
                             {:title "Bokstavsrim (PDF)"
                              :body "Tekst..."
                              :url "http://download.com/bokstavsrim.pdf"
                              :type "examination/measures"
                              :locale "nb-NO"}]}

                   ;; setup question
                   {:insert-into :question_question
                    :values [{:name "Test question 1"
                              :transcript "What is the first letter in the word cat?"
                              :answer "c"
                              :display_duration 10000
                              :locale "nb-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 2"
                              :transcript "What is the first letter in the word giraffe?"
                              :answer "g"
                              :display_duration 10000
                              :locale "nb-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 3"
                              :transcript "What is the first letter in the word dog?"
                              :answer "d"
                              :display_duration 10000
                              :locale "nb-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 4"
                              :transcript "What is the first letter in the word monkey?"
                              :answer "m"
                              :display_duration 10000
                              :locale "nb-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 5"
                              :transcript "What is the first letter in the word bird?"
                              :answer "b"
                              :display_duration 10000
                              :locale "nb-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 1 - nn-NO"
                              :transcript "What is the first letter in the word cat?"
                              :answer "c"
                              :display_duration 10000
                              :locale "nn-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 2 - nn-NO"
                              :transcript "What is the first letter in the word giraffe?"
                              :answer "g"
                              :display_duration 10000
                              :locale "nn-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 3 - nn-NO"
                              :transcript "What is the first letter in the word dog?"
                              :answer "d"
                              :display_duration 10000
                              :locale "nn-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 4 - nn-NO"
                              :transcript "What is the first letter in the word monkey?"
                              :answer "m"
                              :display_duration 10000
                              :locale "nn-NO"
                              :aws_key "question/test.mp3"}]}
                   {:insert-into :question_question
                    :values [{:name "Test question 5 - nn-NO"
                              :transcript "What is the first letter in the word bird?"
                              :answer "b"
                              :display_duration 10000
                              :locale "nn-NO"
                              :aws_key "question/test.mp3"}]}

                   ;; setup an entire track
                   {:insert-into :word_word
                    :values (map (fn [text]
                                   {:text text
                                    :locale "nb-NO"
                                    :display_duration (+ 4000 (rand-int 6000))
                                    :pause_before_start (+ 1000 (rand-int 4000))})
                                 words)}
                   {:insert-into :word_word
                    :values (map (fn [text]
                                   {:text text
                                    :locale "nn-NO"
                                    :display_duration (+ 4000 (rand-int 6000))
                                    :pause_before_start (+ 1000 (rand-int 4000))})
                                 words)}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 1 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 1
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 1
                                    :word_id (inc idx)
                                    :ordering idx
                                    :serial 1})
                                 (range 6 10))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 2 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 2
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   ;; add nn-NO words to exam 2
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 2
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10 20))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 3 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 3
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 4 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 4
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 5 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 5
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 6 testing"
                              :type "word"}]}
                   {:insert-into :examination_words
                    :values (map (fn [idx]
                                   {:examination_id 6
                                    :word_id (inc idx)
                                    :ordering idx})
                                 (range 10))}
                   {:insert-into :examination_examination
                    :values [{:name "Examination 7 testing"
                              :type "question"}]}
                   {:insert-into :examination_questions
                    :values (map (fn [idx]
                                   {:examination_id 7
                                    :question_id (inc idx)
                                    :ordering idx
                                    :serial 0})
                                 (->> (range 10)
                                      (partition 5)
                                      (apply interleave)
                                      (partition 2)
                                      (take 2)
                                      (flatten)))}
                   {:insert-into :examination_questions
                    :values (map (fn [idx]
                                   {:examination_id 7
                                    :question_id (inc idx)
                                    :ordering idx
                                    :serial 1})
                                 (->> (range 10)
                                      (partition 5)
                                      (apply interleave)
                                      (partition 2)
                                      (drop 2)
                                      (flatten)))}
                   {:insert-into :examination_examination
                    :values (map (fn [idx]
                                   {:name (str "Examination " idx " testing")
                                    :type "word"})
                                 (range 8 40))}
                   {:insert-into :examination_words
                    :values (flatten
                             (map (fn [idx-exam]
                                    (map (fn [idx]
                                           {:examination_id idx-exam
                                            :word_id (inc idx)
                                            :ordering idx})
                                         (range 10)))
                                  (range 8 40)))}
                   {:insert-into :examination_informational
                    :values [{:examination_id 1
                              :informational_id 1
                              :ordering 0}
                             {:examination_id 1
                              :informational_id 2
                              :ordering 0}
                             {:examination_id 2
                              :informational_id 3
                              :ordering 1}
                             {:examination_id 2
                              :informational_id 4
                              :ordering 1}
                             {:examination_id 2
                              :informational_id 9
                              :ordering 0}
                             {:examination_id 2
                              :informational_id 10
                              :ordering 0}
                             {:examination_id 1
                              :informational_id 11
                              :ordering 0}]}

                   ;; dimension
                   {:insert-into :dimension_dimension
                    :values [{:name "Fonologisk bevissthet"
                              :short_id "A"
                              :ordering 1
                              :parent nil}
                             {:name "Fonologisk ordavkodning"
                              :short_id "B"
                              :ordering 2
                              :parent nil}
                             {:name "Ortografisk mønstre#Ortografisk ordavkodning"
                              ;; quick/dirty hack to avoid a bunch of work to introduce
                              ;; the idea of a group name different from name: the rule
                              ;; being "name[#group-name]"
                              :short_id "C"
                              :ordering 3
                              :parent nil}
                             {:name "Høyfrekvente irregulære ord"
                              :short_id "D"
                              :ordering 1
                              :parent 3}
                             {:name "Høyfrekvente regulære ord"
                              :short_id "E"
                              :ordering 2
                              :parent 3}]}

                   {:insert-into :track_track
                    :values [{:name "Track testing"
                              :description "Description track testing"
                              :ordering 0
                              :dimension_id 1
                              :active_p true}]}
                   {:insert-into :track_examinations
                    :values (into
                             [{:track_id 1
                               :examination_id 1
                               :ordering 0}
                              {:track_id 1
                               :examination_id 2
                               :ordering 1}
                              {:track_id 1
                               :examination_id 3
                               :ordering 2}
                              {:track_id 1
                               :examination_id 4
                               :ordering 3}
                              {:track_id 1
                               :examination_id 5
                               :ordering 4}
                              {:track_id 1
                               :examination_id 6
                               :ordering 5}
                              {:track_id 1
                               :examination_id 7
                               :ordering 6}]
                             (map (fn [idx]
                                    {:track_id 1
                                     :examination_id idx
                                     :ordering (dec idx)})
                                  (range 8 40)))
                    }

                   ;; setup an assignment
                   {:insert-into :assignment_assignment
                    :values [{:teacher_id 1
                              :due_date (t/now)
                              :message "Assignment 1 message testing"
                              :examination_id 1}]}
                   {:insert-into :assignment_assignment
                    :values [{:teacher_id 1
                              :due_date (t/now)
                              :message "Assignment 2 message testing"
                              :examination_id 2}]}
                   {:insert-into :assignment_assignment
                    :values [{:teacher_id 1
                              :due_date (t/now)
                              :message "Assignment 3 message testing"
                              :examination_id 1}]}
                   ;; question examination
                   {:insert-into :assignment_assignment
                    :values [{:teacher_id 1
                              :due_date (t/now)
                              :message "Assignment 4 message question"
                              :examination_id 7}]}
                   {:insert-into :assignment_students
                    :values [{:assignment_id 1
                              :student_id 1
                              :status ""}
                             {:assignment_id 1
                              :student_id 2
                              :status ""}
                             {:assignment_id 1
                              :student_id 3
                              :status ""}
                             {:assignment_id 1
                              :student_id 4
                              :status "done"}
                             {:assignment_id 2
                              :student_id 3
                              :status ""}
                             {:assignment_id 2
                              :student_id 4
                              :status ""}
                             {:assignment_id 3
                              :student_id 5
                              :status ""}
                             {:assignment_id 3
                              :student_id 1
                              :status ""
                              :examination_serial 1}
                             {:assignment_id 4
                              :student_id 7
                              :status ""
                              :examination_serial 0}
                             {:assignment_id 4
                              :student_id 8
                              :status ""
                              :examination_serial 0}]}

                   ;; results and scoring
                   {:insert-into :result_result
                    :values (reduce (fn [out values]
                                      (into out values))
                                    ;; empty
                                    []
                                    
                                    [
                                     [;; student 1, 2 and 3 for assignment 1
                                      ;; only one result sent in per student
                                      {:assignment_id 1
                                       :word_id 1
                                       :student_id 1}
                                      {:assignment_id 1
                                       :word_id 6
                                       :student_id 2}
                                      {:assignment_id 1
                                       :word_id 5
                                       :student_id 3}]
                                     ;; student 5 has finished the first assignment
                                     (map (fn [idx]
                                            {:assignment_id 3
                                             :word_id (inc idx)
                                             :student_id 5})
                                          (range 10))
                                     ;; student 4 has finished the second assignment
                                     (map (fn [idx]
                                            {:assignment_id 2
                                             :word_id (inc idx)
                                             :student_id 4})
                                          (range 10))])}
                   {:insert-into :result_result
                    :values (reduce (fn [out values]
                                      (into out values))
                                    ;; empty
                                    []
                                    
                                    [;; student 7
                                     (map (fn [idx]
                                            {:assignment_id 4
                                             :question_id idx
                                             :student_id 7})
                                          ;; only the first two questions are used
                                          ;; as they are locale nb-NO
                                          [1 2])])}
                   ;; student 5/assignment 1
                   {:insert-into :audio_audio
                    :values (map (fn [[idx word]]
                                   {:aws_key (str "test/" word ".wav")
                                    :result_id (+ idx 4)})
                                 (map vector (range (count words)) words))}
                   ;; student 4/assignment 2
                   {:insert-into :audio_audio
                    :values (map (fn [[idx word]]
                                   {:aws_key (str "test/" word ".wav")
                                    :result_id (+ idx 14)})
                                 (map vector (range (count words)) words))}
                   ;; we have scored student 5/assignment 1
                   {:insert-into :scoring_scoring
                    :values (into [{:score 1
                                    :result_id 4
                                    :teacher_id 1
                                    :text ""}
                                   {:score 1
                                    :result_id 5
                                    :teacher_id 1
                                    :text ""}]
                                  (map (fn [x]
                                         {:score 0
                                          :result_id (+ x 6)
                                          :teacher_id 1
                                          :text ""})
                                       (range 8)))}
                   ]]
      (db/query! db query)))
  ;; add superuser
  (model.auth/add-user! db #:auth.user{:email "test@songpark.no" :first-name "Test" :last-name "McTest" :password "testme" :active? true})
  (log/set-level! :info))
