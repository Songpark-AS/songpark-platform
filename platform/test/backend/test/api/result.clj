(ns platform.test.api.result
  (:require [midje.sweet :refer :all]
            [platform.api.result :as api.result]
            [platform.model.audio :as model.audio]
            [platform.model.result :as model.result]
            [platform.test.util :as util :refer [b64-encode]]
            [taoensso.timbre :as log]))


(defn- add-body
  ([request body]
   (assoc-in request [:parameters :body] body))
  ([request body student-id]
   (-> request
       (assoc-in [:parameters :body] body)
       (assoc-in [:session :identity :auth.user/id] student-id))))

(fact "result"
      (let [db (util/start-db)
            aws (util/start-aws)
            google (util/start-google)
            word-body {:assignment/id 1
                       :word/id 2
                       :locale/id "nb-NO"
                       :audio/base64 (b64-encode "test-audio/ski.wav")}
            question-body {:assignment/id 4
                           :question/id 1
                           :locale/id "nb-NO"
                           :audio/base64 (b64-encode "test-audio/answer-is-c.wav")}
            missing-in-body {:assignment/id 1
                             :locale/id "nb-NO"
                             :audio/base64 (b64-encode "test-audio/ski.wav")}
            too-much-in-body {:assignment/id 1
                              :question/id 1
                              :word/id 1
                              :locale/id "nb-NO"
                              :audio/base64 (b64-encode "test-audio/ski.wav")}
            ;; student #1 does not have this assignment at all
            invalid-assignment {:assignment/id 4
                                :question/id 2
                                :locale/id "nb-NO"
                                :audio/base64 (b64-encode "test-audio/ski.wav")}
            invalid-assignment-wrong-type {:assignment/id 1
                                           :question/id 2
                                           :locale/id "nb-NO"
                                           :audio/base64 (b64-encode "test-audio/ski.wav")}
            request {:data {:database db
                            :aws aws
                            :google google}
                     ;; set it as -1 so that the folder created in AWS is separate from all the other
                     :session {:identity {:auth.user/id 1}}}]
        (try
          (util/seed-db! db)

          (fact "save-result - faked"
                (with-redefs-fn {#'model.result/save-result                       (fn [_ _]
                                                                                    true)
                                 #'model.audio/save-audio                         (fn [_ _ _]
                                                                                    true)
                                 #'platform.api.result/send-to-google-speech2text! (fn [_ _ _]
                                                                                    {:transcript true
                                                                                     :confidence 1.223
                                                                                     :word-infos nil})
                                 #'platform.model.transcription/save-transcription (fn [_ _ _]
                                                                                    true)
                                 #'platform.model.result/save-result-transcription (fn [_ _ _]
                                                                                    true)}
                  #(do
                     (api.result/save-result (add-body request word-body))
                     => {:status 200
                         :body {:result :success}})))
          (fact "save-result"
                (fact "word"
                      (api.result/save-result (add-body request word-body))
                      => {:status 200
                          :body {:result :success}})
                (fact "question"
                      (api.result/save-result (add-body request question-body 8))
                      => {:status 200
                          :body {:result :success}})
                (fact "no word or question"
                      (api.result/save-result (add-body request missing-in-body))
                      => {:status 400
                          :body {:error/message "Missing both :question/id and :word/id in the result. At least one must be present."}})
                (fact "both word and question"
                      (api.result/save-result (add-body request too-much-in-body))
                      => {:status 400
                          :body {:error/message "Both :question/id and :word/id are present in the result. Only one must be present."}})
                ;; TODO: Implement a new version of trigger_result_insert_before_check()
                ;; that checks that the student is indeed assigned to the assignment
                #_(fact "invalid assignment, student doesn't have the assignment"
                        (try
                          (api.result/save-result (add-body request invalid-assignment))
                          (catch Exception e
                            (log/debug e)
                            true))
                        => true)
                (fact "invalid assignment, wrong type"
                      (try
                        (api.result/save-result (add-body request invalid-assignment-wrong-type))
                        (catch Exception e
                          (log/debug e)
                          true))
                      => true))
          
          (finally
            (util/stop-db db)
            (util/stop-aws aws)
            (util/stop-google google)))))
