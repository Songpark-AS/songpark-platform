(ns platform.test.auth.authz
  (:require [midje.sweet :refer :all]
            [platform.auth.authz :as authz]
            [platform.http.middleware :refer [wrap-exceptions
                                             wrap-authz
                                             unauthorized-handler]]
            [platform.test.util]
            [buddy.auth.platforms :as platforms]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [taoensso.timbre :as log]))

(def default-response {:status 200
                       :body "It worked"
                       :headers {"Content-Type" "plain/text; charset=utf-8"}})
(def error-response   {:status 403
                       :body "Permission denied"})
(defn authz-handler [request]
  default-response)


(fact
 "authz"
 (fact "add namespace to roles"
       (let [roles #{::foo :foo}]
         (authz/add-namespaces roles))
       => #{::foo :foo :platform.test.auth.authz})
 (fact "allow? base implementation"
       (fact "true"
             (let [super nil
                   credentials #{:staff}
                   roles #{:staff/get :staff/post}]
               (authz/-allow? super credentials roles) => true))
       (fact "false"
             (try
               (let [super nil
                     credentials #{:foo}
                     roles #{:staff/get :staff/post}]
                 (authz/-allow? super credentials roles))
               (catch Exception e
                 (get-in (ex-data e) [:buddy.auth/payload :message])))
             => "Credentials not accepted")
       (fact "super user"
             (let [super true
                   credentials #{}
                   roles #{:staff/get :staff/post}]
               (authz/-allow? super credentials roles) => true)))

 (fact "allow? protocol implementation"
       (fact "true"
             (let [user {:authz/super nil
                         :authz/credentials #{:staff}}
                   roles #{:staff/get :staff/post}]
               (authz/allow? user roles) => true))
       (fact "false"
             (try
               (let [user {:authz/super nil
                           :authz/credentials #{:foo}}
                     roles #{:staff/get :staff/post}]
                 (authz/allow? user roles))
               (catch Exception e
                 (get-in (ex-data e) [:buddy.auth/payload :message])))
             => "Credentials not accepted")
       (fact "super user"
             (let [user {:authz/super true
                         :authz/credentials #{:foo}}
                   roles #{:staff/get :staff/post}]
               (authz/allow? user roles) => true))))


(fact
 "authz middleware"
 (let [platform (platforms/session {:unauthorized-handler unauthorized-handler})
       ;; the reitit create-expection-middleware does not quite
       ;; follow the normal convetions for ring. do this to get
       ;; the actual wrap
       wrap (:wrap wrap-exceptions)
       handler (-> authz-handler
                   (wrap-authz #{:student})
                   (wrap-authorization platform)
                   (wrap-authentication platform)
                   wrap)
       saved-log-level (:level log/*config*)]
   ;; so that we don't spam the log with warnings
   (log/set-level! :report)
   (fact "normal roles"
         (fact "authorized"
               (let [request {:session {:identity {:student/id 1
                                                   :authz/credentials #{:student}}}}]
                 (handler request) => default-response))
         (fact "denied"
               (let [request {:session {:identity {:teacher/id 1
                                                   :authz/credentials #{:teacher}}}}]
                 (handler request) => error-response)))
   (fact "super"
         (fact "authorized"
               (let [request {:session {:identity {:auth.user/id 1
                                                   :authz/credentials nil
                                                   :authz/super true}}}]
                 (handler request) => default-response))
         (fact "denied"
               (let [request {:session {:identity {:auth.user/id 1
                                                   :authz/credentials nil
                                                   :authz/super false}}}]
                 (handler request) => error-response)))
   (log/set-level! saved-log-level)))
