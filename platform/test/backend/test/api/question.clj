(ns platform.test.api.question
  (:require [midje.sweet :refer :all]
            [platform.api.question :as api.question]
            [platform.model.question :as model.question]
            [platform.test.util :as util :refer [b64-encode]]
            [clojure.string :as str])
  (:import [java.nio.file Files]))

(defn- url? [v]
  (str/starts-with? v "https://mpluss-taleopptak-test.s3.eu-central-1.amazonaws.com/question/"))

(fact "question"
      (let [db (util/start-db)
            aws (util/start-aws)
            google (util/start-google)
            body {:question/name "Test question 1"
                  :question/transcript "What is the first letter in the word cat?"
                  :question/answer "c"
                  :question.timing/display-duration 10000
                  :locale/id "nb-NO"
                  :audio/base64 (b64-encode "test-audio/What is the first letter in the word Cat?.mp3")}
            expected-result (-> body
                                (dissoc :audio/base64))
            request {:data {:database db
                            :aws aws}
                     :parameters {:body body}
                     :session {:identity {:auth.user/id 1}}}]
        (try
          (util/seed-db! db)
          (fact "create question"
                (let [{:keys [status body]} (api.question/create-question request)]
                  [status
                   (dissoc body
                           :question/audio-url
                           :question/id)
                   (url? (:question/audio-url body))
                   (number? (:question/id body))])
                => [201 expected-result true true])
          
          (finally
            (util/stop-db db)
            (util/stop-aws aws)
            (util/stop-google google)))))

