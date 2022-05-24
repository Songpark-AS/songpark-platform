(ns platform.email.mandrill
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [org.httpkit.client :as client]
            [platform.config :refer [config]]
            [taoensso.timbre :as log]))

(defn post
  ([path data]
   (post path data nil))
  ([path data ?cb]
   ;; if we're dealing with large datasets it can be expensive in
   ;; terms of time and money to send out lots of emails. set
   ;; :debug :mandrill to try to avoid this
   (let [key (get-in config [:mandrill :api-key])
         url (str "https://mandrillapp.com/api/1.0" path)
         data {:headers {"Content-Type" "application/json"}
               :timeout 2000
               :body (json/generate-string (merge {:key key} data))
               :user-agent "Songpark"}]
     (if (nil? ?cb)
       (client/post url data)
       (client/post url data ?cb)))))

(comment
  @(post "/messages/send"
         {:message {:from_email "no-reply@live.songpark.com"
                    :from_name "Songpark Live"
                    :subject "This is a test email"
                    :text "nt my dear friend *|NAME|* with user id *|USERID|* and token *|TOKEN|*"
                    :to [{:email "emil0r@gmail.com" :type "to"}]
                    :merge true
                    :merge_vars [{:rcpt "emil0r@gmail.com"
                                  :vars [{:name "NAME"
                                          :content "Emil Bengtsson"}
                                         {:name "USERID"
                                          :content "1"}
                                         {:name "TOKEN"
                                          :content (str (java.util.UUID/randomUUID))}]}]}}
         println)
  @(post "/messages/send-template"
         {:template_name "verification-of-email"
          :template_content []
          :message {:to [{:email "emil0r@gmail.com" :type "to"}]
                    :merge true
                    :merge_vars [{:rcpt "emil0r@gmail.com"
                                  :vars [{:name "NAME"
                                          :content "Emil Bengtsson"}
                                         {:name "VERIFICATION_URL"
                                          :content (platform.util/get-url "email-verification" {:token (java.util.UUID/randomUUID)})}]}]}}
         println)
  )
