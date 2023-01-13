(ns platform.email.mailchimp
  (:refer-clojure :exclude [get])
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [org.httpkit.client :as client]
            [platform.config :refer [config]]
            [taoensso.timbre :as log]))

;; mail lists
;; Testlist 1: 0921d6896d

(defn post
  ([path data]
   (post path data nil))
  ([path data ?cb]
   (let [api-key (get-in config [:mailchimp :api-key])
         [key dc] (str/split api-key #"-")
         url (str "https://" dc ".api.mailchimp.com/3.0" path)
         data {:timeout 2000
               :headers {"Authorization" (str "Bearer " key)}
               :body (json/generate-string data)
               :user-agent "Songpark"}]
     (if (nil? ?cb)
       (client/post url data)
       (client/post url data ?cb)))))

(defn get
  ([path data]
   (get path data nil))
  ([path data ?cb]
   (let [api-key (get-in config [:mailchimp :api-key])
         [key dc] (str/split api-key #"-")
         url (str "https://" dc ".api.mailchimp.com/3.0" path)
         data {:timeout 2000
               :headers {"Authorization" (str "Bearer " key)}
               :query-params data
               :user-agent "Songpark"}]
     (if (nil? ?cb)
       (client/get url data)
       (client/get url data ?cb)))))

(defn- adapt-merge-fields [merge-fields]
  (->> merge-fields
       (map (fn [[k v]]
              [(str/upper-case (name k)) v]))
       (into {})))

(defn lists-batch-members
  ([users] (lists-batch-subscribe users {}))
  ([users opts]
   (let [size 500
         list-id (get-in config [:mailchimp :list-id])
         url (str "/lists/" list-id)]
     (doseq [n (range (inc (quot (count users) size)))]
       (let [batch-users (take (+ size (* n size))
                               (drop (* n size) users))]
         (post
          url
          (merge
           {:members (->> batch-users
                          (map (fn [{:keys [email email_type
                                            status merge_fields]}]
                                 {:email_address email
                                  :email_type (or email_type "html")
                                  :status (or status "subscribed")
                                  :merge_fields (adapt-merge-fields merge_fields)}))
                          (doall))}
           opts)))))))


(comment

  (lists-batch-members [{:email "emil0r@gmail.com"
                         :status "unsubscribed"
                         :merge_fields {:id 1
                                        :name "Emil Bengtsson"}}]
                       {:update_existing true})
  )
